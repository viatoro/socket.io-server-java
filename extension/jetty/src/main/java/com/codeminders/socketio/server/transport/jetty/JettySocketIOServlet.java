package com.codeminders.socketio.server.transport.jetty;

import com.codeminders.socketio.server.SocketIOServlet;

import javax.servlet.ServletException;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */
public abstract class JettySocketIOServlet extends SocketIOServlet
{
    @Override
    public void init() throws ServletException
    {
        super.init();
        setTransportProvider(new JettyTransportProvider());
    }
}
