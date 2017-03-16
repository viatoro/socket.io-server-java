package com.codeminders.socketio.sample.chat.websocket;

import com.codeminders.socketio.server.SocketIOServlet;
import com.codeminders.socketio.server.TransportProvider;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * Created by asolod on 09.03.17.
 */
public class WebSocketIOServlet  extends SocketIOServlet
{
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        TransportProvider transportProvider = new WebSocketTransportProvider();
        transportProvider.init(config, getServletContext());
        setTransportProvider(transportProvider);
    }
}