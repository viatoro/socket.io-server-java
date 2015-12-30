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

import java.util.concurrent.*;

public final class SocketIOSessionManager implements SessionManager
{
    private static final int SESSION_ID_LEN = 20;
    private static final char[] symbols;

    static
    {
        StringBuilder sb = new StringBuilder();
        for (char ch = 'A'; ch <= 'Z'; ch++)
            sb.append(ch);
        for (char ch = 'a'; ch <= 'z'; ch++)
            sb.append(ch);
        symbols = sb.toString().toCharArray();
    }

    final ConcurrentMap<String, SocketIOSession> socketIOSessions = new ConcurrentHashMap<>();
    final ScheduledExecutorService               executor         = Executors.newScheduledThreadPool(1);

    private String gennerateSessionId()
    {
        while(true)
        {
            StringBuilder sb = new StringBuilder(SESSION_ID_LEN);
            for (int i = 0; i < SESSION_ID_LEN; i++)
                sb.append(symbols[ThreadLocalRandom.current().nextInt(symbols.length)]);

            String id = sb.toString();
            if(socketIOSessions.get(id) == null)
                return id;
        }

    }

    @Override
    public SocketIOSession createSession(SocketIOInbound inbound)
    {
        DefaultSession impl = new DefaultSession(this, inbound, gennerateSessionId());
        socketIOSessions.put(impl.getSessionId(), impl);
        return impl;
    }

    @Override
    public SocketIOSession getSession(String sessionId)
    {
        return socketIOSessions.get(sessionId);
    }
}