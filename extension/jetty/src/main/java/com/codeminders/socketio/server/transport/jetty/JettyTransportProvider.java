package com.codeminders.socketio.server.transport.jetty;

import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.TransportConnection;
import com.codeminders.socketio.server.transport.AbstractTransportProvider;
import com.codeminders.socketio.server.transport.FlashSocketTransport;
import com.codeminders.socketio.server.transport.JSONPPollingTransport;
import com.codeminders.socketio.server.transport.XHRPollingTransport;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */
public class JettyTransportProvider extends AbstractTransportProvider
{

    @Override
    protected Transport createWebSocketTransport()
    {
        return new JettyWebSocketTransport();
    }

    protected Transport createFlashSocketTransport(Transport delegate)
    {
        return new FlashSocketTransport(delegate);
    }

    protected Transport createXHTPollingTransport()
    {
        return new XHRPollingTransport()
        {
            @Override
            public TransportConnection createConnection()
            {
                return new JettyContinuationTransportConnection(this);
            }
        };
    }

    protected Transport createJSONPPollingTransport()
    {
        return new JSONPPollingTransport()
        {
            @Override
            public TransportConnection createConnection()
            {
                return new JettyContinuationTransportConnection(this);
            }
        };
    }
}
