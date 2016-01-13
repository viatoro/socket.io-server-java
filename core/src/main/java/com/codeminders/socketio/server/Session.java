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
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: this class is not thread-safe at all

/**
 * SocketIO session.
 * <p/>
 * This implementation is not thread-safe.
 *
 * @author Alexander Sova (bird@codeminders.com)
 */
public class Session implements DisconnectListener
{
    private static final Logger LOGGER = Logger.getLogger(Session.class.getName());

    private final SocketIOManager socketIOManager;
    private final String          sessionId;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    //TODO: should we allow multiple sockets for the same namespace in a session?
    private Map<String, Socket> sockets = new LinkedHashMap<>(); // namespace, socket

    private TransportConnection connection;
    private ConnectionState  state            = ConnectionState.CONNECTING;
    private DisconnectReason disconnectReason = DisconnectReason.UNKNOWN;
    private String disconnectMessage;

    private long      timeout;
    private Future<?> timeoutTask;
    private boolean   timedOut;

    private BinaryPacket binaryPacket;
    private int                       packet_id     = 0; // packet id. used for requesting ACK
    private Map<Integer, ACKListener> ack_listeners = new LinkedHashMap<>(); // packetid, listener

    Session(SocketIOManager socketIOManager, String sessionId)
    {
        assert (socketIOManager != null);

        this.socketIOManager = socketIOManager;
        this.sessionId = sessionId;
    }

    public Socket createSocket(String ns)
    {
        Namespace namespace = socketIOManager.getNamespace(ns);
        if (namespace == null)
            throw new IllegalArgumentException("Namespace does not exist");

        Socket socket = namespace.createSocket(this);
        socket.on(this); // listen for disconnect event
        sockets.put(ns, socket);
        return socket;
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

        timeoutTask = socketIOManager.executor.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                Session.this.onTimeout();
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

        if (engineIOPacket.getType() != EngineIOPacket.Type.MESSAGE)
            throw new SocketIOProtocolException("Unexpected binary packet type. Type: " + engineIOPacket.getType());

        if (binaryPacket == null)
            throw new SocketIOProtocolException("Unexpected binary object");

        SocketIOProtocol.insertBinaryObject(binaryPacket, engineIOPacket.getBinaryData());
        binaryPacket.addAttachment(engineIOPacket.getBinaryData()); //keeping copy of all attachments in attachments list
        if (binaryPacket.isComplete())
        {
            if (binaryPacket.getType() == SocketIOPacket.Type.BINARY_EVENT)
                onEvent((EventPacket) binaryPacket);
            else if (binaryPacket.getType() == SocketIOPacket.Type.BINARY_ACK)
                onACK((ACKPacket) binaryPacket);

            binaryPacket = null;
        }
    }

    public void onConnect(TransportConnection connection) throws SocketIOException
    {
        assert (connection != null);
        assert (this.connection == null); //TODO: may not be the case for upgrade.

        this.connection = connection;

        Socket socket = createSocket(SocketIOProtocol.DEFAULT_NAMESPACE);
        try
        {
            state = ConnectionState.CONNECTED;
            socketIOManager.getNamespace(SocketIOProtocol.DEFAULT_NAMESPACE).onConnect(socket);
        }
        catch (ConnectionException e)
        {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Connection failed", e);

            connection.send(SocketIOProtocol.createErrorPacket(SocketIOProtocol.DEFAULT_NAMESPACE, e.getArgs()));
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
     */
    public void onShutdown()
    {
        if (state == ConnectionState.CLOSING)
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

        if (state == ConnectionState.CLOSED)
            return; // to prevent calling it twice

        state = ConnectionState.CLOSED;

        clearTimeout();

        for (Socket socket : sockets.values())
            socket.onDisconnect(socket, reason, disconnectMessage);

        socketIOManager.deleteSession(sessionId);
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
                try
                {
                    if (socketIOManager.getNamespace(packet.getNamespace()) == null)
                    {
                        getConnection().send(SocketIOProtocol.createErrorPacket(packet.getNamespace(), "Invalid namespace"));
                        return;
                    }
                    Socket socket = createSocket(packet.getNamespace());
                    getConnection().send(SocketIOProtocol.createConnectPacket(packet.getNamespace()));
                    try
                    {
                        socketIOManager.getNamespace(socket.getNamespace()).onConnect(socket);
                    }
                    catch (ConnectionException e)
                    {
                        getConnection().send(SocketIOProtocol.createErrorPacket(socket.getNamespace(), e.getArgs()));
                        socket.disconnect(false);
                    }
                }
                catch (SocketIOException e)
                {
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(Level.FINE, "Cannot send packet to the client", e);

                    closeConnection(DisconnectReason.CONNECT_FAILED);
                }
                return;

            case DISCONNECT:
                closeConnection(DisconnectReason.CLOSED_REMOTELY);
                return;

            case EVENT:
                onEvent((EventPacket) packet);
                return;

            case ACK:
                onACK((ACKPacket) packet);
                return;

            case BINARY_ACK:
            case BINARY_EVENT:
                binaryPacket = (BinaryPacket) packet;
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

    private void onEvent(EventPacket packet)
    {
        if (state != ConnectionState.CONNECTED)
            return;

        try
        {
            Namespace ns = socketIOManager.getNamespace(packet.getNamespace());
            if (ns == null)
            {
                getConnection().send(SocketIOProtocol.createErrorPacket(packet.getNamespace(), "Invalid namespace"));
                return;
            }

            Socket socket = sockets.get(ns.getId());
            if (socket == null)
            {
                connection.send(SocketIOProtocol.createErrorPacket(packet.getNamespace(),
                        "No socket is connected to the namespace"));
                return;
            }

            Object ack = socket.onEvent(packet.getName(), packet.getArgs());

            if (packet.getId() != -1 && ack != null)
            {
                Object[] args;
                if (ack instanceof Objects[])
                    args = (Object[]) ack;
                else
                    args = new Object[]{ack};

                connection.send(SocketIOProtocol.createACKPacket(packet.getId(), packet.getNamespace(), args));
            }
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by one of the event listeners", e);
        }
    }

    private void onACK(ACKPacket packet)
    {
        if (state != ConnectionState.CONNECTED)
            return;

        try
        {
            ACKListener listener = ack_listeners.get(packet.getId());
            unsubscribeACK(packet.getId());
            if (listener != null)
                listener.onACK(packet.getArgs());
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by Inbound.onEvent()", e);
        }
    }

    /**
     * Remembers the disconnect reason and closes underlying transport connection
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
    public void subscribeACK(int packet_id, ACKListener ack_listener)
    {
        ack_listeners.put(packet_id, ack_listener);
    }

    //TODO: to be called when ACK is received
    public void unsubscribeACK(int packet_id)
    {
        ack_listeners.remove(packet_id);
    }

    @Override
    public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage)
    {
        sockets.remove(socket.getNamespace());
    }
}
