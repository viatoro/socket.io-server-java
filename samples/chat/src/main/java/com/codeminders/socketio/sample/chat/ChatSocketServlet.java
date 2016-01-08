/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
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
package com.codeminders.socketio.sample.chat;

import com.codeminders.socketio.server.SocketIOACKListener;
import com.codeminders.socketio.server.SocketIOInbound;
import com.codeminders.socketio.server.transport.jetty.JettySocketIOServlet;
import com.codeminders.socketio.util.IO;
import com.codeminders.socketio.util.JdkOverLog4j;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.server.SocketIOOutbound;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatSocketServlet extends JettySocketIOServlet
{
    private static final String CHAT_MESSAGE_EVENT    = "chat message";       // test message
    private static final String WELCOME_MESSAGE_EVENT = "welcome";
    private static final String FORCE_DISCONNECT      = "force disconnect";   // request server to disconnect
    private static final String SERVER_BINARY         = "server binary";      // request server to send a binary
    private static final String CLIENT_BINARY         = "client binary";      // client sends binary

    private static final Logger LOGGER = Logger.getLogger(ChatSocketServlet.class.getName());

    private static final long serialVersionUID = 1L;
    private AtomicInteger ids = new AtomicInteger(1);
    private Queue<ChatConnection> connections = new ConcurrentLinkedQueue<ChatConnection>();

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        JdkOverLog4j.install();
        super.init(config);
    }

    private class ChatConnection implements SocketIOInbound
    {
        private volatile SocketIOOutbound outbound = null;
        private Integer sessionId = ids.getAndIncrement();

        @Override
        public void onConnect(SocketIOOutbound outbound)
        {
            LOGGER.fine("Client connected");
            this.outbound = outbound;
            connections.offer(this);

            try
            {
                outbound.emit(WELCOME_MESSAGE_EVENT, "Welcome to Socket.IO Chat!");
            }
            catch (SocketIOException e)
            {
                LOGGER.log(Level.SEVERE, "Cannot send a chat message", e);
                outbound.disconnect();
            }
        }

        @Override
        public void onDisconnect(DisconnectReason reason, String errorMessage)
        {
            this.outbound = null;
            connections.remove(this);
            //broadcast(Collections.singletonMap("announcement", sessionId + " disconnected"));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object onEvent(String name, Object[] args)
        {
            LOGGER.fine("Got event: " + name);

            //TODO: allow user to subscribe to specific events, like .on(CHAT_MESSAGE_EVENT, new SocketIOEventListener() {...})
            if(CHAT_MESSAGE_EVENT.equals(name))
            {
                if (args.length > 0)
                    broadcast(CHAT_MESSAGE_EVENT, sessionId.toString(), args[0]);

                return new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }); // send this as an acknowledgement if client requested
            }
            else
            if (FORCE_DISCONNECT.equals(name))
            {
                outbound.disconnect();
            }
            else
            if(CLIENT_BINARY.equals(name))
            {
                Map map = (Map<Object, Object>)args[0];
                InputStream is = (InputStream) map.get("buffer");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try
                {
                    IO.copy(is, os);
                    byte []array = os.toByteArray();
                    System.out.print("[");
                    for (byte b : array)
                        System.out.print(" " + b);
                    System.out.println(" ]");

                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            else
            if(SERVER_BINARY.equals(name))
            {
                try
                {
                    outbound.emit(SERVER_BINARY, new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }), new SocketIOACKListener()
                    {
                        @Override
                        public void onACK(Object[] args)
                        {
                            System.out.println("ACK recieved: " + args[0]);
                        }
                    });
                }
                catch (SocketIOException e)
                {
                    outbound.disconnect();
                }
            }

            return null;
        }

        private void broadcast(String name, Object... args)
        {
            for (ChatConnection c : connections)
            {
                if (c == this)
                    continue;

                try
                {
                    c.outbound.emit(name, args);
                }
                catch (IOException e)
                {
                    c.outbound.disconnect();
                }
            }
        }
    }

    @Override
    protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
        return new ChatConnection();
    }

}
