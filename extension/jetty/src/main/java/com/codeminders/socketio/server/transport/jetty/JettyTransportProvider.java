package com.codeminders.socketio.server.transport.jetty;

import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.transport.AbstractTransportProvider;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class JettyTransportProvider extends AbstractTransportProvider
{

    @Override
    protected Transport createWebSocketTransport()
    {
        return new JettyWebSocketTransport();
    }

//TODO: disabling all the transports except Websocket for now
//    protected Transport createXHTPollingTransport()
//    {
//        return new XHRPollingTransport()
//        {
//            @Override
//            public TransportConnection createConnection()
//            {
//                return new JettyContinuationTransportConnection(this);
//            }
//        };
//    }
//
//    protected Transport createJSONPPollingTransport()
//    {
//        return new JSONPPollingTransport()
//        {
//            @Override
//            public TransportConnection createConnection()
//            {
//                return new JettyContinuationTransportConnection(this);
//            }
//        };
//    }
}
