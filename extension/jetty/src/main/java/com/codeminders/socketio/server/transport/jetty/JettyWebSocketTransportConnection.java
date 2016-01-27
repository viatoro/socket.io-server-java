/**
 * The MIT License
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
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

import com.codeminders.socketio.protocol.*;
import com.codeminders.socketio.server.*;
import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

import com.codeminders.socketio.server.transport.AbstractTransportConnection;
import com.google.common.io.ByteStreams;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */

@WebSocket
public final class JettyWebSocketTransportConnection extends AbstractTransportConnection
{
    private static final Logger LOGGER = Logger.getLogger(JettyWebSocketTransportConnection.class.getName());

    private org.eclipse.jetty.websocket.api.Session remote_endpoint;

    public JettyWebSocketTransportConnection(Transport transport)
    {
        super(transport);
    }

    @Override
    protected void init()
    {
        getSession().setTimeout(getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT));

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getConfig().getNamespace() + " WebSocket Connection configuration:\n" +
                    " - timeout=" + getSession().getTimeout());
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(org.eclipse.jetty.websocket.api.Session session)
    {
        remote_endpoint = session;

        if(getSession().getConnectionState() == ConnectionState.CONNECTING)
        {
            try
            {
                send(EngineIOProtocol.createHandshakePacket(getSession().getSessionId(),
                        new String[]{},
                        getConfig().getPingInterval(Config.DEFAULT_PING_INTERVAL),
                        getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT)));

                getSession().onConnect(this);
            }
            catch (SocketIOException e)
            {
                LOGGER.log(Level.SEVERE, "Cannot connect", e);
                getSession().setDisconnectReason(DisconnectReason.CONNECT_FAILED);
                abort();
            }
        }
    }

    @OnWebSocketClose
    public void onWebSocketClose(int closeCode, String message)
    {
        if(LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Websocket closed. Close code: " + closeCode + " message: " + message);

        //If close is unexpected then try to guess the reason based on closeCode, otherwise the reason is already set
        if(getSession().getConnectionState() != ConnectionState.CLOSING)
            getSession().setDisconnectReason(fromCloseCode(closeCode));

        getSession().setDisconnectMessage(message);
        getSession().onShutdown();
    }

    @OnWebSocketMessage
    public void onWebSocketText(String text)
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Text received: " + text);
        getSession().resetTimeout();

        try
        {
            getSession().onPacket(EngineIOProtocol.decode(text), this);
        }
        catch (SocketIOProtocolException e)
        {
            LOGGER.log(Level.WARNING, "Invalid packet received", e);
        }
    }

    @OnWebSocketMessage
    public void onWebSocketBinary(InputStream is)
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Binary received");
        getSession().resetTimeout();

        try
        {
            getSession().onPacket(EngineIOProtocol.decode(is), this);
        }
        catch (SocketIOProtocolException e)
        {
            LOGGER.log(Level.WARNING, "Problem processing binary received", e);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected request on upgraded WebSocket connection");
    }

    @Override
    public void abort()
    {
        getSession().clearTimeout();
        if (remote_endpoint != null)
        {
            disconnectEndpoint();
            remote_endpoint = null;
        }
    }

    @Override
    public void send(EngineIOPacket packet) throws SocketIOException
    {
        sendString(EngineIOProtocol.encode(packet));
    }

    @Override
    public void send(SocketIOPacket packet) throws SocketIOException
    {
        send(EngineIOProtocol.createMessagePacket(packet.encode()));
        if(packet instanceof BinaryPacket)
        {
            Collection<InputStream> attachments = ((BinaryPacket) packet).getAttachments();
            for (InputStream is : attachments)
            {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try
                {
                    os.write(EngineIOPacket.Type.MESSAGE.value());
                    ByteStreams.copy(is, os);
                }
                catch (IOException e)
                {
                    if(LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.SEVERE, "Cannot load binary object to send it to the socket", e);
                }
                sendBinary(os.toByteArray());
            }
        }
    }

    protected void sendString(String data) throws SocketIOException
    {
        if (!remote_endpoint.isOpen())
            throw new SocketIOClosedException();

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE,
                    "Session[" + getSession().getSessionId() + "]: sendString: " + data);
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

    //TODO: implement streaming. right now it is all in memory.
    //TODO: read and send in chunks using sendPartialBytes()
    protected void sendBinary(byte[] data) throws SocketIOException
    {
        if (!remote_endpoint.isOpen())
            throw new SocketIOClosedException();

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE,
                    "Session[" + getSession().getSessionId() + "]: sendBinary");
        try
        {
            remote_endpoint.getRemote().sendBytes(ByteBuffer.wrap(data));
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

    private DisconnectReason fromCloseCode(int code)
    {
        switch (code)
        {
            case StatusCode.SHUTDOWN:
                return DisconnectReason.CLIENT_GONE;
            default:
                return DisconnectReason.ERROR;
        }
    }
}
