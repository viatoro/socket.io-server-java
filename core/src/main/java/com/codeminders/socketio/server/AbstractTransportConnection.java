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

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

/**
 * @author Mathieu Carbou
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class AbstractTransportConnection implements TransportConnection
{
    private SocketIOConfig config;
    private SocketIOSession session;

    @Override
    public final void init(SocketIOConfig config) {
        this.config = config;
        init();
    }

    @Override
    public void setSession(SocketIOSession session) {
        this.session = session;
    }

    protected final SocketIOConfig getConfig() {
        return config;
    }

    protected final SocketIOSession getSession() {
        return session;
    }

    protected void init()
    {
    }

    public void send(SocketIOPacket packet) throws SocketIOException
    {
        send(EngineIOProtocol.createMessagePacket(SocketIOProtocol.encode(packet)));
    }

    @Override
    public void disconnect()
    {
        try
        {
            send(SocketIOProtocol.createDisconnectPacket());
            getSession().setDisconnectReason(DisconnectReason.DISCONNECT);
        }
        catch (SocketIOException e)
        {
            getSession().setDisconnectReason(DisconnectReason.CLOSE_FAILED);
        }

        abort();
    }
}
