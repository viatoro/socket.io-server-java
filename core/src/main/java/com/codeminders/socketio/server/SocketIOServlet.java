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
package com.codeminders.socketio.server;

import com.codeminders.socketio.server.transport.AbstractTransportProvider;
import com.codeminders.socketio.util.IO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SocketIOServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SocketIOServlet.class.getName());
    private static final long serialVersionUID = 2L;

    private final SocketIOSessionManager sessionManager = new SocketIOSessionManager();

    private TransportProvider transportProvider;

    public final static String MAX_TEXT_MESSAGE_SIZE     = "maxTextMessageSize";

    public void setTransportProvider(TransportProvider transportProvider) {
        assert (transportProvider != null);
        this.transportProvider = transportProvider;
    }

    @Override
    public void init() throws ServletException
    {
        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.log(Level.INFO, "Transports: " + transportProvider.getTransports());
    }

    @Override
    public void destroy()
    {
        transportProvider.destroy();
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        serve(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        serve(req, resp);
    }

    /**
     * Returns an instance of SocketIOInbound or null if the connection is to be denied.
     * The value of cookies and protocols may be null.
     */
    protected abstract SocketIOInbound doSocketIOConnect(HttpServletRequest request);

    private void serve(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        //TODO: Need to remove this check. Transport is not defined in path anymore
        String path = request.getPathInfo();
        if (path == null || path.length() == 0 || "/".equals(path)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing SocketIO transport");
            return;
        }
        if (path.startsWith("/")) path = path.substring(1);
        String[] parts = path.split("/");

        if ("GET".equals(request.getMethod()) && "socket.io.js".equals(parts[0])) {
            response.setContentType("text/javascript");
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("com/codeminders/socketio/socket.io.js");
            OutputStream os = response.getOutputStream();
            IO.copy(is, os);
        }
        else
        if ("GET".equals(request.getMethod()) && "WebSocketMain.swf".equals(parts[0])) {
            response.setContentType("application/x-shockwave-flash");
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("com/codeminders/socketio/WebSocketMain.swf");
            OutputStream os = response.getOutputStream();
            IO.copy(is, os);
        }
        else
        {
            assert (transportProvider != null);

            Transport transport = null;
            try
            {
                transport = transportProvider.getTransport(request);
            } catch (UnsupportedTransportException | SocketIOProtocolException e)
            {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot find appropriate transport");

                if(LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Cannot find appropriate transport", e);
                return;
            }

            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Handling request from " +
                        request.getRemoteHost() + ":" + request.getRemotePort() +
                        " with transport: " + transport.getType());

            transport.handle(request, response, new Transport.InboundFactory() {
                @Override
                public SocketIOInbound getInbound(HttpServletRequest request) {
                    return SocketIOServlet.this.doSocketIOConnect(request);
                }
            }, sessionManager);
        }
    }

}
