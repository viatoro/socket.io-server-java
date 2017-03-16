package com.codeminders.socketio.sample.chat.websocket;

import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.transport.AbstractTransportProvider;

/**
 * Created by asolod on 09.03.17.
 */
public class WebSocketTransportProvider extends AbstractTransportProvider {

    @Override
    protected Transport createWebSocketTransport() {
        return new WebSocketTransport();
    }

}
