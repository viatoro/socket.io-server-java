package com.codeminders.socketio.server.onemore;

import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.transport.AbstractTransportProvider;

/**
 * Created by asolod on 18.03.17.
 */
public class WebsocketTransportProvider extends AbstractTransportProvider {

    @Override
    protected Transport createWebSocketTransport()
    {
        return new WebsocketTransport();
    }

}
