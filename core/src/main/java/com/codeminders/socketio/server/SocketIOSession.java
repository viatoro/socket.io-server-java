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
import com.codeminders.socketio.util.JSON;
import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alexander Sova (bird@codeminders.com)
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class SocketIOSession
{
    private static final Logger LOGGER = Logger.getLogger(SocketIOSession.class.getName());

    private final SocketIOSessionManager socketIOSessionManager;
    private final String                 sessionId;
    private final Map<String, Object>    attributes = new ConcurrentHashMap<>();

    //TODO: rename to something more telling
    //This is callback/listener interface set by library user
    private SocketIOInbound inbound;

    private TransportConnection connection;
    private ConnectionState state = ConnectionState.CONNECTING;

    private long        timeout;
    private Future<?>   timeoutTask;
    private boolean     timedOut;
    private String      closeId;

    SocketIOSession(SocketIOSessionManager socketIOSessionManager, SocketIOInbound inbound, String sessionId)
    {
        this.socketIOSessionManager = socketIOSessionManager;
        this.inbound = inbound;
        this.sessionId = sessionId;
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

    public SocketIOInbound getInbound()
    {
        return inbound;
    }

    public TransportConnection getConnection()
    {
        return connection;
    }

    private void onTimeout()
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onTimeout");

        if (!timedOut)
        {
            timedOut = true;
            state = ConnectionState.CLOSED;
            onDisconnect(DisconnectReason.TIMEOUT);
            connection.abort();
        }
    }

    public void startTimeoutTimer()
    {
        clearTimeoutTimer();
        if (timedOut || timeout == 0)
            return;

        timeoutTask = socketIOSessionManager.executor.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                SocketIOSession.this.onTimeout();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public void clearTimeoutTimer()
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

    //TODO: remove or rename to disconnect()
    public void startClose()
    {
        state = ConnectionState.CLOSING;
        closeId = "server";
//        try
//        {
//            //TODO: use new protocol
//            connection.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, closeId));
//        }
//        catch (SocketIOException e)
//        {
//            if (LOGGER.isLoggable(Level.FINE))
//                LOGGER.log(Level.FINE, "connection.send failed: ", e);
//
//            connection.abort();
//        }
    }

    public void onPacket(String data)
            throws SocketIOProtocolException
    {
        onPacket(EngineIOProtocol.decode(data));
    }

    public void onConnect(TransportConnection connection)
    {
        if (connection == null)
        {
            state = ConnectionState.CLOSED;
            inbound = null;
            socketIOSessionManager.socketIOSessions.remove(sessionId);
        }
        else
        if(this.connection == null)
        {
            this.connection = connection;
            if (inbound == null)
            {
                state = ConnectionState.CLOSED;
                connection.abort();
            }
            else
            {
                try
                {
                    state = ConnectionState.CONNECTED;
                    inbound.onConnect(connection);
                }
                catch (Throwable e)
                {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onConnect()", e);
                    state = ConnectionState.CLOSED;
                    connection.abort();
                }
            }
        }
        else
            connection.abort();
    }

    public void onDisconnect(DisconnectReason reason)
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onDisconnect: " + reason);

        clearTimeoutTimer();
        if (inbound != null)
        {
            state = ConnectionState.CLOSED;
            try
            {
                inbound.onDisconnect(reason, null);
            }
            catch (Throwable e)
            {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onDisconnect()", e);
            }
            inbound = null;
        }
    }

    public void onShutdown()
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onShutdown");

        if (inbound != null)
        {
            if (state == ConnectionState.CLOSING)
            {
                if (closeId != null)
                    onDisconnect(DisconnectReason.CLOSE_FAILED);
                else
                    onDisconnect(DisconnectReason.CLOSED_REMOTELY);
            }
            else
                onDisconnect(DisconnectReason.ERROR);
        }
        socketIOSessionManager.socketIOSessions.remove(sessionId);
    }



    private void onPacket(EngineIOPacket packet)
    {
        switch (packet.getType())
        {
            case OPEN:
            case PONG:
                // ignore. OPEN and PONG are server -> client
                return;
            case MESSAGE:
                startTimeoutTimer();
                try
                {
                    onPacket(SocketIOProtocol.decode(packet.getData()));
                }
                catch (SocketIOProtocolException e)
                {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Invalid SIO packet: " + packet.getData(), e);
                }
                return;

            case PING:
                startTimeoutTimer();
                onPing(packet.getData());
                return;

            case CLOSE:
                //TODO: never tested. the client sends SIO DISCONNECT packet on socket.close()
                startClose();
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
                // ignore. server -> client
                return;
            case DISCONNECT:
                startClose();
                return;
            case EVENT:
                Object json = JSON.parse(packet.getData());
                if (!(json instanceof Object[]) || ((Object[]) json).length == 0)
                {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Invalid JSON in EVENT message packet: " + packet.getData());
                    return;
                }

                Object[] args = (Object[]) json;
                if (!(args[0] instanceof String))
                {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Invalid JSON in EVENT message packet. First argument must be string: " + packet.getData());
                    return;
                }
                onEvent(args[0].toString(), Arrays.copyOfRange(args, 1, args.length));

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

            connection.abort();
        }
    }

    private void onEvent(String name, Object[] args)
    {
        if(inbound == null)
            return;

        try
        {
            inbound.onEvent(name, args);
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onEvent()", e);
        }
    }
}
