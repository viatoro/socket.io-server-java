/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 *
 * Contributors: Ovea.com, Mycila.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.codeminders.socketio.server.transport;

import com.codeminders.socketio.server.*;
import com.codeminders.socketio.util.Web;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractHttpTransport extends AbstractTransport {

    private static final Logger LOGGER = Logger.getLogger(AbstractHttpTransport.class.getName());
    public static final String SESSION_KEY = AbstractHttpTransport.class.getName() + ".Session";

    @Override
    public final void handle(HttpServletRequest request,
                             HttpServletResponse response,
                             InboundFactory inboundFactory,
                             SessionManager sessionFactory) throws IOException
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Handling request " + request.getRequestURI() + " by " + getClass().getName());

        SocketIOSession session = null;
        String sessionId = Web.extractSessionId(request);
        if (sessionId != null && sessionId.length() > 0)
        {
            session = sessionFactory.getSession(sessionId);
        }

        if (session != null)
        {
            TransportConnection handler = session.getTransportHandler();
            if (handler != null)
            {
                handler.handle(request, response, session);
            }
            else
            {
                session.onShutdown();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        else
        {
            if ("GET".equals(request.getMethod()))
            {
                try
                {
                    session = connect(request, response, inboundFactory,
                                      sessionFactory, sessionId);
                } catch (SocketIOProtocolException e)
                {
                    if(LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Cannot initialize connection", e);
                }
                if (session == null)
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            else
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private SocketIOSession connect(HttpServletRequest request,
                                    HttpServletResponse response,
                                    InboundFactory inboundFactory,
                                    SessionManager sessionFactory,
                                    String sessionId) throws IOException, SocketIOProtocolException
    {
        SocketIOInbound inbound = inboundFactory.getInbound(request);
        if (inbound != null)
        {
            if (sessionId == null)
                sessionId = request.getSession().getId();

            SocketIOSession session = sessionFactory.createSession(inbound, sessionId);

            //TODO: this is wierd. need to refactor connection sequence
            createConnection(session).connect(request, response);
            connect(session, request, response);

            return session;
        }

        return null;
    }

    public abstract void startSend(SocketIOSession session, ServletResponse response) throws IOException;

    public abstract void writeData(SocketIOSession session, ServletResponse response, String data) throws IOException;

    public abstract void finishSend(SocketIOSession session, ServletResponse response) throws IOException;

    public abstract void connect(SocketIOSession session, ServletRequest request, ServletResponse response)
                throws IOException, SocketIOProtocolException;
}
