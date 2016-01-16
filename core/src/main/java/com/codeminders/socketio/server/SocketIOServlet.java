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
package com.codeminders.socketio.server;

import com.codeminders.socketio.protocol.SocketIOProtocol;
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

public abstract class SocketIOServlet extends HttpServlet
{
    private static final Logger LOGGER = Logger.getLogger(SocketIOServlet.class.getName());

    private TransportProvider transportProvider;
    private final SocketIOManager socketIOManager = new SocketIOManager();

    public Namespace of(String id)
    {
        Namespace ns = socketIOManager.getNamespace(id);
        if (ns == null)
            ns = socketIOManager.createNamespace(id);

        return ns;
    }

    public void setTransportProvider(TransportProvider transportProvider)
    {
        assert (transportProvider != null);
        this.transportProvider = transportProvider;
    }

    @Override
    public void init() throws ServletException
    {
        if (LOGGER.isLoggable(Level.INFO))
        {
            if (transportProvider != null)
            {
                LOGGER.log(Level.INFO, "Transports: " + transportProvider.getTransports());
                if (transportProvider.getTransports().size() == 0)
                    LOGGER.log(Level.INFO, "No transport defined. TransportProvider.init() should be called.");
            }
            else
                LOGGER.log(Level.INFO, "No Transport Provider is set");
        }

        of(SocketIOProtocol.DEFAULT_NAMESPACE);
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

    private void serve(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String path = request.getPathInfo();

        if (path.startsWith("/")) path = path.substring(1);
        String[] parts = path.split("/");

        if ("GET".equals(request.getMethod()) && "socket.io.js".equals(parts[0]))
        {
            response.setContentType("text/javascript");
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("com/codeminders/socketio/socket.io.js");
            OutputStream os = response.getOutputStream();
            IO.copy(is, os);
        }
        else
        {
            assert (transportProvider != null);

            try
            {
                Transport transport = transportProvider.getTransport(request);

                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Handling request from " +
                            request.getRemoteHost() + ":" + request.getRemotePort() +
                            " with transport: " + transport.getType());

                transport.handle(request, response, socketIOManager);
            }
            catch (UnsupportedTransportException | SocketIOProtocolException e)
            {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot find appropriate transport");

                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Cannot find appropriate transport", e);
            }
        }
    }
}
