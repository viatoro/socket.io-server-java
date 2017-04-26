/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p/>
 * Contributors: Ovea.com, Mycila.com
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

public final class WebsocketTransport extends AbstractTransport
{
    private static final Logger LOGGER = Logger.getLogger(WebsocketTransport.class.getName());

    @Override
    public void init(ServletConfig config, ServletContext context)
            throws ServletException
    {
        super.init(config, context);
        // TODO
        /*
        wsFactory.getPolicy().setMaxTextMessageSize(getConfig().getInt(Config.MAX_TEXT_MESSAGE_SIZE, 32000));
        wsFactory.getPolicy().setInputBufferSize(getConfig().getBufferSize());
        wsFactory.getPolicy().setIdleTimeout(getConfig().getMaxIdle());

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getType() + " configuration:\n" +
                    " - bufferSize=" + wsFactory.getPolicy().getInputBufferSize() + "\n" +
                    " - maxIdle=" + wsFactory.getPolicy().getIdleTimeout());
                    */
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
        // TODO
        /*

        wsFactory.acceptWebSocket(new WebSocketCreator() {
                @Override
                public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
                                              ServletUpgradeResponse servletUpgradeResponse)
                {
                    return connection;
                }
            }, request, response);
         */
    }

    @Override
    public TransportConnection createConnection()
    {
        return new WebsocketTransportConnection(this);
    }
}
