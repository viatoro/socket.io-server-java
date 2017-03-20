package com.codeminders.socketio.server.onemore;

import com.codeminders.socketio.protocol.EngineIOProtocol;
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
 * Created by asolod on 17.03.17.
 */
public class WebsocketTransport extends AbstractTransport {

    private static final Logger LOGGER = Logger.getLogger(WebsocketTransport.class.getName());

    @Override
    public void init(ServletConfig config, ServletContext context)
            throws ServletException
    {
        super.init(config, context);

/*        wsFactory.getPolicy().setMaxTextMessageSize(getConfig().getInt(Config.MAX_TEXT_MESSAGE_SIZE, 32000));
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

        final TransportConnection connection = getConnection(request.getParameter(EngineIOProtocol.SESSION_ID), sessionManager);

        // a bit hacky but safe since we know the type of TransportConnection here
        ((AbstractTransportConnection)connection).setRequest(request);
    }

    @Override
    public TransportConnection createConnection()
    {
        return new WebsocketTransportConnection(this);
    }
}
