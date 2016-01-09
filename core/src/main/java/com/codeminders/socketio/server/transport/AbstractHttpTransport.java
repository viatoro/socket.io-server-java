/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * <p/>
 * Contributors: Tad Glines, Ovea.com, Mycila.com, Alexander Sova (bird@codeminders.com)
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
package com.codeminders.socketio.server.transport;

import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.server.*;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractHttpTransport extends AbstractTransport
{
    private static final Logger LOGGER  = Logger.getLogger(AbstractHttpTransport.class.getName());

    @Override
    public final void handle(HttpServletRequest request,
                             HttpServletResponse response,
                             InboundFactory inboundFactory,
                             SessionManager sessionFactory)
            throws IOException
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Handling request " + request.getRequestURI() + " by " + getClass().getName());

        Session session = null;
        String sessionId = request.getParameter(EngineIOProtocol.SESSION_ID);
        if (sessionId != null && sessionId.length() > 0)
            session = sessionFactory.getSession(sessionId);

        if (session != null)
        {
            TransportConnection connection = session.getConnection();
            if (connection != null)
            {
                connection.handle(request, response, session);
            }
            else
            {
//                session.onDisconnect(DisconnectReason.ERROR);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        else
        {
            if (!"GET".equals(request.getMethod()))
            {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }


            try
            {
                Inbound inbound = inboundFactory.getInbound(request);
                if (inbound == null)
                {
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    return;
                }

                session = sessionFactory.createSession(inbound);
                //TODO: wierd sequence. need to refactor
                createConnection(session).connect(request, response);
                onConnect(session, request, response);
            }
            catch (SocketIOProtocolException e)
            {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Cannot initialize connection", e);
            }
            if (session == null)
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    public abstract void startSend(Session session, ServletResponse response) throws IOException;

    public abstract void writeData(Session session, ServletResponse response, String data) throws IOException;

    public abstract void finishSend(Session session, ServletResponse response) throws IOException;

    public abstract void onConnect(Session session, ServletRequest request, ServletResponse response)
            throws IOException, SocketIOProtocolException;
}
