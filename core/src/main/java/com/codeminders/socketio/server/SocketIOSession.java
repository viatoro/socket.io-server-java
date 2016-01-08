/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p/>
 * Contributors: Ovea.com, Mycila.com
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.codeminders.socketio.server;

import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.*;
import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: this class is not thread-safe at all

/**
 * SocketIO session.
 *
 * This implementation is not thread-safe.
 *
 * @author Alexander Sova (bird@codeminders.com)
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class SocketIOSession
{
    private static final Logger LOGGER = Logger.getLogger(SocketIOSession.class.getName());

    private final SocketIOSessionManager sessionManager;
    private final String                 sessionId;
    private final Map<String, Object>    attributes = new ConcurrentHashMap<>();

    //TODO: rename to something more telling
    //This is callback/listener interface set by library user
    private SocketIOInbound inbound;

    private TransportConnection connection;
    private ConnectionState  state = ConnectionState.CONNECTING;
    private DisconnectReason disconnectReason = DisconnectReason.UNKNOWN;
    private String           disconnectMessage;

    private long        timeout;
    private Future<?>   timeoutTask;
    private boolean     timedOut;

    private SocketIOBinaryPacket socketIOBinaryPacket;
    private int packet_id = 0; // packet id. used for requesting ACK
    private Map<Integer, SocketIOACKListener> ack_listeners = new LinkedHashMap<>();

    SocketIOSession(SocketIOSessionManager sessionManager, SocketIOInbound inbound, String sessionId)
    {
        assert (sessionManager != null);

        this.sessionManager = sessionManager;
        this.inbound        = inbound;
        this.sessionId      = sessionId;
    }

    public void setAttribute(String key, Object val)
    {
        attributes.put(key, val);
    }

    public Object getAttribute(String key)
    {
        return attributes.get(key);
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public ConnectionState getConnectionState()
    {
        return state;
    }

    public TransportConnection getConnection()
    {
        return connection;
    }

    public void resetTimeout()
    {
        clearTimeout();
        if (timedOut || timeout == 0)
            return;

        timeoutTask = sessionManager.executor.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                SocketIOSession.this.onTimeout();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public void clearTimeout()
    {
        if (timeoutTask != null)
        {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    public long getTimeout()
    {
        return timeout;
    }

    public void onText(String data)
            throws SocketIOProtocolException
    {
        onPacket(EngineIOProtocol.decode(data));
    }

    public void onBinary(InputStream is)
        throws SocketIOProtocolException
    {
        EngineIOPacket engineIOPacket = EngineIOProtocol.decode(is);

        if(engineIOPacket.getType() != EngineIOPacket.Type.MESSAGE)
            throw new SocketIOProtocolException("Unexpected binary packet type. Type: " + engineIOPacket.getType());

        if(socketIOBinaryPacket == null)
            throw new SocketIOProtocolException("Unexpected binary object");

        SocketIOProtocol.insertBinaryObject(socketIOBinaryPacket, engineIOPacket.getBinaryData());
        socketIOBinaryPacket.addAttachment(engineIOPacket.getBinaryData()); //keeping copy of all attachments in attachments list
        if(socketIOBinaryPacket.isComplete())
        {
            if(socketIOBinaryPacket.getType() == SocketIOPacket.Type.EVENT)
                onEvent((SocketIOEventPacket) socketIOBinaryPacket);
            else
            if(socketIOBinaryPacket.getType() == SocketIOPacket.Type.BINARY_ACK)
                onACK((SocketIOACKPacket) socketIOBinaryPacket);

            socketIOBinaryPacket = null;
        }
    }

    public void onConnect(TransportConnection connection)
    {
        assert (connection != null);
        assert (this.connection == null); //TODO: may not be the case for upgrade.

        this.connection = connection;

        if (inbound == null)
        {
            //this could happen only if onDisconnect was called already
            closeConnection(DisconnectReason.CONNECT_FAILED);
            return;
        }

        try
        {
            state = ConnectionState.CONNECTED;
            inbound.onConnect(connection);
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onConnect()", e);

            closeConnection(DisconnectReason.CONNECT_FAILED);
        }
    }

    /**
     * Optional. if transport know detailed error message it could be set before calling onShutdown()
     */
    public void setDisconnectMessage(String message)
    {
        this.disconnectMessage = message;
    }

    /**
     * Calling this method will change connection status to CLOSING!
     */
    public void setDisconnectReason(DisconnectReason reason)
    {
        this.state = ConnectionState.CLOSING;
        this.disconnectReason = reason;
    }

    /**
     * callback to be called by transport connection socket is closed.
     *
     */
    public void onShutdown()
    {
        if(state == ConnectionState.CLOSING)
            onDisconnect(disconnectReason);
        else
            onDisconnect(DisconnectReason.ERROR);
    }

    //TODO: race condition on disconnect from remote endpoint
    /**
     * Disconnect callback. to be called by session itself. Transport connection should always call onShutdown()
     */
    private void onDisconnect(DisconnectReason reason)
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onDisconnect: " + reason +
                    " message: [" + disconnectMessage + "]");

        //TODO: this is not enough. inbound is not protected
        synchronized(this)
        {
            if (state == ConnectionState.CLOSED)
                return; // to prevent calling it twice

            state = ConnectionState.CLOSED;
        }

        clearTimeout();

        if (inbound != null)
        {
            try
            {
                inbound.onDisconnect(reason, disconnectMessage);
            }
            catch (Throwable e)
            {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onDisconnect()", e);
            }
            inbound = null;
        }
        sessionManager.deleteSession(sessionId);
    }

    private void onTimeout()
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onTimeout");

        if (!timedOut)
        {
            timedOut = true;
            closeConnection(DisconnectReason.TIMEOUT);
        }
    }

    private void onPacket(EngineIOPacket packet)
    {
        switch (packet.getType())
        {
            case OPEN:
            case PONG:
                // ignore. OPEN and PONG are server -> client only
                return;

            case MESSAGE:
                resetTimeout();
                try
                {
                    onPacket(SocketIOProtocol.decode(packet.getTextData()));
                }
                catch (SocketIOProtocolException e)
                {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Invalid SIO packet: " + packet.getTextData(), e);
                }
                return;

            case PING:
                resetTimeout();
                onPing(packet.getTextData());
                return;

            case CLOSE:
                //TODO: never tested. the client sends SIO DISCONNECT packet on socket.close()
                closeConnection(DisconnectReason.CLOSED_REMOTELY);
                return;

            default:
                throw new UnsupportedOperationException("EIO Packet " + packet + " is not implemented yet");

        }
    }

    private void onPacket(SocketIOPacket packet)
    {
        switch (packet.getType())
        {
            case CONNECT:
                // ignore. server -> client only
                return;

            case DISCONNECT:
                closeConnection(DisconnectReason.CLOSED_REMOTELY);
                return;

            case EVENT:
                onEvent((SocketIOEventPacket)packet);
                return;

            case ACK:
                onACK((SocketIOACKPacket)packet);
                return;

            case BINARY_ACK:
            case BINARY_EVENT:
                socketIOBinaryPacket = (SocketIOBinaryPacket)packet;
                return;

            default:
                throw new UnsupportedOperationException("SocketIO packet " + packet.getType() + " is not implemented yet");
        }
    }

    private void onPing(String data)
    {
        try
        {
            connection.send(EngineIOProtocol.createPongPacket(data));
        }
        catch (SocketIOException e)
        {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "connection.send failed: ", e);

            closeConnection(DisconnectReason.ERROR);
        }
    }

    private void onEvent(SocketIOEventPacket packet)
    {
        //TODO: not thread-safe. synchronize
        if(inbound == null || state != ConnectionState.CONNECTED)
            return;

        try
        {
            Object ack = inbound.onEvent(packet.getName(), packet.getArgs());

            if(packet.getId() != -1 && ack != null)
            {
                Object[] args;
                if(ack instanceof Objects[])
                    args = (Object[])ack;
                else
                    args = new Object[] { ack };

                connection.send(SocketIOProtocol.createACKPacket(packet.getId(), args));
            }
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onEvent()", e);
        }
    }

    private void onACK(SocketIOACKPacket packet)
    {
        if(inbound == null || state != ConnectionState.CONNECTED)
            return;

        try
        {
            SocketIOACKListener listener = ack_listeners.get(packet.getId());
            unsubscribeACK(packet.getId());
            if(listener != null)
                listener.onACK(packet.getArgs());
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onEvent()", e);
        }
    }

    /**
     * remembers the disconnect reason and closes underlying transport connection
     */
    private void closeConnection(DisconnectReason reason)
    {
        setDisconnectReason(reason);
        connection.abort(); //this call should trigger onShutdown() eventually
    }

    public synchronized int getNewPacketId()
    {
        return packet_id++;
    }

    //TODO: what if ACK never comes? We will have a memory leak. Need to cleanup the list or fail on timeout?
    public void subscribeACK(int packet_id, SocketIOACKListener ack_listener)
    {
        ack_listeners.put(packet_id, ack_listener);
    }

    //TODO: to be called when ACK is received
    public void unsubscribeACK(int packet_id)
    {
        ack_listeners.remove(packet_id);
    }
}
