package com.codeminders.socketio.server.transport.websocket;

import com.codeminders.socketio.server.SocketIOManager;
import com.codeminders.socketio.server.TransportConnection;
import com.codeminders.socketio.server.TransportType;
import com.codeminders.socketio.server.transport.AbstractTransport;
import com.codeminders.socketio.server.transport.AbstractTransportConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by asolod on 09.03.17.
 */
public class WebSocketTransport extends AbstractTransport {

    private static final Logger LOGGER = Logger.getLogger(WebSocketTransport.class.getName());

    //private final WebSocketServerFactory wsFactory = new WebSocketServerFactory();

    @Override
    public void init(ServletConfig config, ServletContext context)
            throws ServletException
    {
        super.init(config, context);

       /* try
        {
            // wsFactory.init(context); //this need to be called for Jetty 9.3.x
            wsFactory.init();
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }

        wsFactory.getPolicy().setMaxTextMessageSize(getConfig().getInt(Config.MAX_TEXT_MESSAGE_SIZE, 32000));
        wsFactory.getPolicy().setInputBufferSize(getConfig().getBufferSize());
        wsFactory.getPolicy().setIdleTimeout(getConfig().getMaxIdle());

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getType() + " configuration:\n" +
                    " - bufferSize=" + wsFactory.getPolicy().getInputBufferSize() + "\n" +
                    " - maxIdle=" + wsFactory.getPolicy().getIdleTimeout());*/
    }

    @Override
    public TransportType getType()
    {
        return TransportType.WEB_SOCKET;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       SocketIOManager sessionManager) throws IOException
    {

        if(!"GET".equals(request.getMethod()))
        {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "Only GET method is allowed for websocket transport");
            return;
        }

        final TransportConnection connection = getConnection(request, sessionManager);

        // a bit hacky but safe since we know the type of TransportConnection here
        ((AbstractTransportConnection)connection).setRequest(request);

        /*wsFactory.acceptWebSocket(new WebSocketCreator() {
            @Override
            public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
                                          ServletUpgradeResponse servletUpgradeResponse)
            {
                return connection;
            }
        }, request, response);*/
    }

    @Override
    public TransportConnection createConnection()
    {
        return new WebSocketTransportConnection(this);
    }
}
