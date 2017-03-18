package com.codeminders.socketio.server.onemore;

import com.codeminders.socketio.server.SocketIOServlet;
import com.codeminders.socketio.server.TransportProvider;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * Created by asolod on 18.03.17.
 */
public class WebsocketServlet extends SocketIOServlet {

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        TransportProvider transportProvider = new WebsocketTransportProvider();
        transportProvider.init(config, getServletContext());
        setTransportProvider(transportProvider);
    }
}
