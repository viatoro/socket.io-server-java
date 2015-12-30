package com.codeminders.socketio.server.transport.jetty;

import com.codeminders.socketio.server.SocketIOServlet;
import com.codeminders.socketio.server.TransportProvider;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */
public abstract class JettySocketIOServlet extends SocketIOServlet
{
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        TransportProvider transportProvider = new JettyTransportProvider();
        transportProvider.init(config);
        setTransportProvider(transportProvider);

        super.init(config);
    }
}
