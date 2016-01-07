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
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    //TODO: not thread-safe at all
    private SocketIOBinaryEventPacket binaryEventPacket;

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
        //TODO: synchronize
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

        if(binaryEventPacket == null)
            throw new SocketIOProtocolException("Unexpected binary object");

        SocketIOProtocol.insertBinaryObject(binaryEventPacket, engineIOPacket.getBinaryData());
        binaryEventPacket.addAttachment(engineIOPacket.getBinaryData()); //keeping copy of all attachments in attachments list
        if(binaryEventPacket.isComplete())
        {
            SocketIOEventPacket packet = binaryEventPacket;
            binaryEventPacket = null; //it's better to reset the state before making potentially long call
            onEvent(packet);
        }

        //TODO: process binary ACK
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
    public void onDisconnect(DisconnectReason reason)
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
                // Session.onDisconnect() is being called twice.
                // this happens because the client sends DISCONNECT packet and immediately drops the connection.
                closeConnection(DisconnectReason.CLOSED_REMOTELY);
                return;

            case EVENT:
                onEvent((SocketIOEventPacket)packet);
                return;

            case BINARY_EVENT:
                binaryEventPacket = (SocketIOBinaryEventPacket)packet;
                return;

            case ACK:
            case BINARY_ACK:
                //TODO: pass the notification to the user
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
                connection.send(EngineIOProtocol.createMessagePacket(
                        SocketIOProtocol.createACKPacket(packet.getId(), ack).encode()
                ));
            }
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
}
