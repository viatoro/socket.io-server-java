/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
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
package com.codeminders.socketio.server.transport.jetty;

import com.codeminders.socketio.server.*;
import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

import com.codeminders.socketio.util.JSON;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */

@WebSocket
public final class JettyWebSocketTransportConnection extends AbstractTransportConnection
{
    private static final Logger LOGGER = Logger.getLogger(JettyWebSocketTransportConnection.class.getName());

    private Session   remote_endpoint;
    private Transport transport;

    public JettyWebSocketTransportConnection(Transport transport)
    {
        this.transport = transport;
    }

    @Override
    protected void init()
    {
        getSession().setTimeout(getConfig().getHeartbeatTimeout(SocketIOConfig.DEFAULT_HEARTBEAT_TIMEOUT));

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getConfig().getNamespace() + " WebSocket Connection configuration:\n" +
                    " - timeout=" + getSession().getTimeout());
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(Session session)
    {
        remote_endpoint = session;
        try
        {
            send(EngineIOProtocol.createHandshakePacket(getSession().getSessionId(),
                    new String[]{},
                    getConfig().getPingInterval(SocketIOConfig.DEFAULT_PING_INTERVAL),
                    getConfig().getTimeout(SocketIOConfig.DEFAULT_PING_TIMEOUT)));

            send(new SocketIOPacket(SocketIOPacket.Type.CONNECT));

        }
        catch (SocketIOException e)
        {
            LOGGER.log(Level.SEVERE, "Cannot onConnect", e);
            getSession().onConnect(null); //TODO: use different callback if connection failed
        }
        getSession().onConnect(this);
    }

    @OnWebSocketClose
    public void onWebSocketClose(int closeCode, String message)
    {
        getSession().onShutdown();
    }

    @OnWebSocketMessage
    public void onWebSocketText(String text)
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Packet received: " + text);

        //TODO: check if it is possible to get multiple packets in one transmission
        //TODO: for now we expect single packet per call
        getSession().startTimeoutTimer();

        try
        {
            getSession().onPacket(text);
        }
        catch (SocketIOProtocolException e)
        {
            LOGGER.log(Level.WARNING, "Invalid packet received", e);
        }
    }

    @OnWebSocketMessage
    public void onWebSocketBinary(byte[] data, int offset, int length)
    {
        //TODO: use proper binary encoding/decoding defined in EIO
        try
        {
            onWebSocketText(new String(data, offset, length, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            // Do nothing for now.
        }
    }

    @Override
    public void disconnect()
    {
        getSession().onDisconnect(DisconnectReason.DISCONNECT);
        disconnectEndpoint();
    }

    @Override
    public void close()
    {
        getSession().startClose();
    }

    @Override
    public ConnectionState getConnectionState()
    {
        return getSession().getConnectionState();
    }

    @Override
    public void send(EngineIOPacket packet) throws SocketIOException
    {
        sendString(EngineIOProtocol.encode(packet));
    }

    @Override
    public void emit(String name, Object... args)
            throws SocketIOException
    {
        if (!remote_endpoint.isOpen() || getSession().getConnectionState() != ConnectionState.CONNECTED)
            throw new SocketIOClosedException();

        send(SocketIOProtocol.createEventPacket(name, args));
    }

    @Override
    public Transport getTransport()
    {
        return transport;
    }

    @Override
    public void connect(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // do nothing
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, SocketIOSession session) throws IOException
    {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected request on upgraded WebSocket connection");
    }

    @Override
    public void abort()
    {
        if (remote_endpoint != null)
        {
            disconnectEndpoint();
            remote_endpoint = null;
        }
        getSession().onShutdown();
    }

    private void sendString(String data) throws SocketIOException
    {
        if (!remote_endpoint.isOpen())
            throw new SocketIOClosedException();

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE,
                    "Session[" + getSession().getSessionId() + "]: sendPacket: " + data);
        try
        {
            remote_endpoint.getRemote().sendString(data);
        }
        catch (IOException e)
        {
            disconnectEndpoint();
            throw new SocketIOException(e);
        }
    }

    private void disconnectEndpoint()
    {
        try
        {
            remote_endpoint.disconnect();
        }
        catch (IOException ex)
        {
            // ignore
        }
    }
}
