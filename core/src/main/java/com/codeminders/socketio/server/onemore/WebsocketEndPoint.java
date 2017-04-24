package com.codeminders.socketio.server.onemore;

import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.server.Session;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import java.io.InputStream;

/**
 * Created by asolod on 20.03.17.
 */
public class WebsocketEndPoint extends Endpoint {

    private WebsocketTransportConnection connection;

    public WebsocketEndPoint() {
        System.out.println("");
    }

    @Override
    public void onOpen(javax.websocket.Session websocketSession, EndpointConfig config) {
        websocketSession.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                    WebsocketEndPoint.this.connection.onWebSocketText(message);
            }
        });

        websocketSession.addMessageHandler(new MessageHandler.Whole<InputStream>() {

            @Override
            public void onMessage(InputStream is) {
                    WebsocketEndPoint.this.connection.onWebSocketBinary(is);
            }
        });

        Session session = AbstractWebsocketServlet.socketIOManager.createSession(websocketSession.getId());
        try {

            // TODO: propogate CodeMinders Connection to Transpor layer
            // create Codeminders connection using shared instance of socketIOManager
            // also check that Codeminders Servlet context config propogates to Communication layer
            this.connection = new WebsocketTransport().getC;
            this.connection.setSession(session);
            this.connection.onConnect(websocketSession);
            session.onConnect(this.connection);
        } catch (SocketIOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClose(javax.websocket.Session websocketSession, CloseReason closeReason) {
        this.connection.onWebSocketClose(websocketSession, closeReason);
    }

}
