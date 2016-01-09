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

import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.*;
import com.codeminders.socketio.util.IO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class AbstractTransportConnection implements TransportConnection
{
    private static final Logger LOGGER = Logger.getLogger(AbstractTransportConnection.class.getName());

    private Config  config;
    private Session session;

    @Override
    public final void init(Config config) {
        this.config = config;
        init();
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    protected final Config getConfig() {
        return config;
    }

    protected final Session getSession() {
        return session;
    }

    protected void init()
    {
    }

    public void send(SocketIOPacket packet) throws SocketIOException
    {
        send(EngineIOProtocol.createMessagePacket(packet.encode()));
        if(packet instanceof BinaryPacket)
        {
            Collection<InputStream> attachments = ((BinaryPacket) packet).getAttachments();
            for (InputStream is : attachments)
            {
                try
                {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    os.write(EngineIOPacket.Type.MESSAGE.value());
                    IO.copy(is, os);
                    sendBinary(os.toByteArray());
                }
                catch (IOException e)
                {
                    if(LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.SEVERE, "Cannot load binary object to send it to the socket", e);
                }
            }
        }
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

    @Override
    public void send(EngineIOPacket packet) throws SocketIOException
    {
        sendString(EngineIOProtocol.encode(packet));
    }

    @Override
    public void emit(String name, Object... args)
            throws SocketIOException
    {
        if (getSession().getConnectionState() != ConnectionState.CONNECTED)
            throw new SocketIOClosedException();

        ACKListener ack_listener = null;
        if(args.length > 0 && args[args.length-1] instanceof ACKListener)
        {
            ack_listener = (ACKListener)args[args.length-1];
            args = Arrays.copyOfRange(args, 0, args.length-1);
        }

        int packet_id = -1;
        if(ack_listener != null)
            packet_id = getSession().getNewPacketId();

        SocketIOPacket packet = SocketIOProtocol.createEventPacket(packet_id, name, args);

        if(packet_id >= 0)
            getSession().subscribeACK(packet_id, ack_listener);

        send(packet);
    }

    protected abstract void sendString(String data) throws SocketIOException;
    protected abstract void sendBinary(byte[] data) throws SocketIOException;
}
