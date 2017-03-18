package com.codeminders.socketio.server.onemore;

import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.EngineIOPacket;
import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.protocol.SocketIOPacket;
import com.codeminders.socketio.server.Config;
import com.codeminders.socketio.server.transport.AbstractTransportConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by asolod on 17.03.17.
 */

@ServerEndpoint("/socket.io")
public class WebsocketTransportConnection extends AbstractTransportConnection {

    private static final Logger LOGGER = Logger.getLogger(WebsocketTransportConnection.class.getName());

    private Session session;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;
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


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

    }

    @Override
    public void abort() {

    }

    @Override
    public void send(EngineIOPacket packet) throws SocketIOException {

    }

    @Override
    public void send(SocketIOPacket packet) throws SocketIOException {

    }
}
