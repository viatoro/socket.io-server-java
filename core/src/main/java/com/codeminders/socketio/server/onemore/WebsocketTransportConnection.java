package com.codeminders.socketio.server.onemore;

/**
 * Created by asolod on 20.03.17.
 */

import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.BinaryPacket;
import com.codeminders.socketio.protocol.EngineIOPacket;
import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.protocol.SocketIOPacket;
import com.codeminders.socketio.server.Config;
import com.codeminders.socketio.server.SocketIOClosedException;
import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.transport.AbstractTransportConnection;
import com.google.common.io.ByteStreams;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebsocketTransportConnection extends AbstractTransportConnection {
    private static final Logger LOGGER = Logger.getLogger(WebsocketTransportConnection.class.getName());

    private javax.websocket.Session websocketSession;

    public WebsocketTransportConnection(Transport transport) {
        super(transport);
    }

    @Override
    protected void init() {
        getSession().setTimeout(getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT));

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getConfig().getNamespace() + " WebSocket configuration:" +
                    " timeout=" + getSession().getTimeout());
    }

    public void onConnect(javax.websocket.Session websocketSession) {
        this.websocketSession = websocketSession;

        if (getSession().getConnectionState() == ConnectionState.CONNECTING) {
            try {
                send(EngineIOProtocol.createHandshakePacket(getSession().getSessionId(),
                        new String[]{},
                        getConfig().getPingInterval(Config.DEFAULT_PING_INTERVAL),
                        getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT)));

                getSession().onConnect(this);
            } catch (SocketIOException e) {
                LOGGER.log(Level.SEVERE, "Cannot connect", e);
                getSession().setDisconnectReason(DisconnectReason.CONNECT_FAILED);
                abort();
            }
        }
    }

    public void onWebSocketClose(Session websocketSession, CloseReason closeReason) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + getSession().getSessionId() + "]:" +
                    " websocket closed. Close code: " + closeReason.getCloseCode() + " message: " + closeReason.getReasonPhrase());

        //If close is unexpected then try to guess the reason based on closeCode, otherwise the reason is already set
        if (getSession().getConnectionState() != ConnectionState.CLOSING)
            getSession().setDisconnectReason(fromCloseCode(closeReason));

        getSession().setDisconnectMessage(closeReason.getReasonPhrase());
        getSession().onShutdown();
    }

    public void onWebSocketText(String text) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Session[" + getSession().getSessionId() + "]: text received: " + text);

        getSession().resetTimeout();

        try {
            getSession().onPacket(EngineIOProtocol.decode(text), this);
        } catch (SocketIOProtocolException e) {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Invalid packet received", e);
        }
    }

    public void onWebSocketBinary(InputStream is) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Session[" + getSession().getSessionId() + "]: binary received");

        getSession().resetTimeout();

        try {
            getSession().onPacket(EngineIOProtocol.decode(is), this);
        } catch (SocketIOProtocolException e) {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Problem processing binary received", e);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected request on upgraded WebSocket connection");
    }

    @Override
    public void abort() {
        getSession().clearTimeout();
        if (this.websocketSession != null) {
            disconnectEndpoint();
            this.websocketSession = null;
        }
    }

    @Override
    public void send(EngineIOPacket packet) throws SocketIOException {
        sendString(EngineIOProtocol.encode(packet));
    }

    @Override
    public void send(SocketIOPacket packet) throws SocketIOException {
        send(EngineIOProtocol.createMessagePacket(packet.encode()));
        if (packet instanceof BinaryPacket) {
            Collection<InputStream> attachments = ((BinaryPacket) packet).getAttachments();
            for (InputStream is : attachments) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    os.write(EngineIOPacket.Type.MESSAGE.value());
                    ByteStreams.copy(is, os);
                } catch (IOException e) {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.SEVERE, "Cannot load binary object to send it to the socket", e);
                }
                sendBinary(os.toByteArray());
            }
        }
    }

    protected void sendString(String data) throws SocketIOException {
        if (!this.websocketSession.isOpen())
            throw new SocketIOClosedException();

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + getSession().getSessionId() + "]: send text: " + data);

        this.websocketSession.getAsyncRemote().sendText(data);
    }

    //TODO: implement streaming. right now it is all in memory.
    //TODO: read and send in chunks using sendPartialBytes()
    protected void sendBinary(byte[] data) throws SocketIOException {
        if (!this.websocketSession.isOpen())
            throw new SocketIOClosedException();

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + getSession().getSessionId() + "]: send binary");

        this.websocketSession.getAsyncRemote().sendBinary(ByteBuffer.wrap(data));
    }

    private void disconnectEndpoint() {
        try {
            this.websocketSession.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    private DisconnectReason fromCloseCode(CloseReason closeReason) {
        switch (closeReason.getCloseCode().getCode()) {
     /*       case CloseReason.CloseCodes.CANNOT_ACCEPT.getCode():
                return DisconnectReason.CLIENT_GONE;*/
            default:
                return DisconnectReason.ERROR;
        }
    }
}
