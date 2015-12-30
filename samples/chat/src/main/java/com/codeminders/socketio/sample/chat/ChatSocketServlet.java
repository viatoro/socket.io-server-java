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

import com.codeminders.socketio.server.SocketIOInbound;
import com.codeminders.socketio.server.transport.jetty.JettySocketIOServlet;
import com.codeminders.socketio.util.JdkOverLog4j;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.server.SocketIOOutbound;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatSocketServlet extends JettySocketIOServlet
{
    private static final String CHAT_MESSAGE_EVENT = "chat message";
    private static final String WELCOME_MESSAGE_EVENT = "welcome";

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

//            broadcast(SocketIOFrame.JSON_MESSAGE_TYPE, new Gson().toJson(
//                    Collections.singletonMap("announcement", sessionId + " connected")));
        }

        @Override
        public void onDisconnect(DisconnectReason reason, String errorMessage)
        {
            this.outbound = null;
            connections.remove(this);
            //broadcast(Collections.singletonMap("announcement", sessionId + " disconnected"));
        }


//        public void onMessage(int messageType, String message)
//        {
//            if (message.equals("/rclose"))
//            {
//                outbound.close();
//            }
//            else if (message.equals("/rdisconnect"))
//            {
//                outbound.disconnect();
//            }
//            else if (message.startsWith("/sleep"))
//            {
//                int sleepTime = 1;
//                String parts[] = message.split("\\s+");
//                if (parts.length == 2)
//                {
//                    sleepTime = Integer.parseInt(parts[1]);
//                }
//                try
//                {
//                    Thread.sleep(sleepTime * 1000);
//                }
//                catch (InterruptedException e)
//                {
//                    // Ignore
//                }
//                try
//                {
//                    outbound.sendMessage(SocketIOFrame.JSON_MESSAGE_TYPE, new Gson().toJson(
//                            Collections.singletonMap("message", "Slept for " + sleepTime + " seconds.")));
//                }
//                catch (SocketIOException e)
//                {
//                    outbound.disconnect();
//                }
//            }
//            else if (message.startsWith("/burst"))
//            {
//                int burstNum = 10;
//                String parts[] = message.split("\\s+");
//                if (parts.length == 2)
//                {
//                    burstNum = Integer.parseInt(parts[1]);
//                }
//                try
//                {
//                    for (int i = 0; i < burstNum; i++)
//                    {
//                        outbound.sendMessage(SocketIOFrame.JSON_MESSAGE_TYPE, new Gson().toJson(
//                                Collections.singletonMap("message", new String[]{"Server", "Hi " + i +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
//                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
//                                })));
////						outbound.sendMessage(SocketIOFrame.JSON_MESSAGE_TYPE, JSON.toString(
////								Collections.singletonMap("say","Hi " + i)));
//                        try
//                        {
//                            Thread.sleep(250);
//                        }
//                        catch (InterruptedException e)
//                        {
//                            // Do nothing
//                        }
//                    }
//                }
//                catch (Exception e)
//                {
////				} catch (SocketIOException e) {
////					outbound.disconnect();
//                }
//            }
//            else
//            {
//                broadcast(
//                        Collections.singletonMap("message",
//                                new String[]{sessionId.toString(), message})));
//            }
//        }

        @Override
        public void onEvent(String name, Object[] args)
        {
            //TODO: log arguments?
            LOGGER.fine("Got event: " + name);

            if(args.length > 0)
                broadcast(CHAT_MESSAGE_EVENT, sessionId.toString(), args[0]);
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
