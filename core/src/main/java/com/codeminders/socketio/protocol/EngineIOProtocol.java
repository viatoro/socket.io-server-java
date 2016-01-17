/**
 * The MIT License
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
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
package com.codeminders.socketio.protocol;

import com.codeminders.socketio.server.SocketIOProtocolException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of Engine.IO Protocol version 3
 *
 * @author Alexander Sova (bird@codeminders.com)
 */
public final class EngineIOProtocol
{
    public static final String SESSION_ID  = "sid";
    public static final String TRANSPORT   = "transport";
    public static final String JSONP_INDEX = "j";
    public static final String BASE64_FLAG = "b64";

    private EngineIOProtocol()
    {
    }

    public static String encode(EngineIOPacket packet)
    {
        return String.valueOf(packet.getType().value()) + packet.getTextData();
    }

    public static EngineIOPacket decode(String raw)
            throws SocketIOProtocolException
    {
        assert (raw != null);

        if(raw.length() < 1)
            throw new SocketIOProtocolException("Empty EIO packet");

        try
        {
            return new EngineIOPacket(
                    EngineIOPacket.Type.fromInt(Integer.parseInt(raw.substring(0, 1))),
                    raw.substring(1));
        }
        catch (NumberFormatException e)
        {
            throw new SocketIOProtocolException("Invalid EIO packet type: " + raw);
        }
    }


    public static EngineIOPacket decode(InputStream raw)
        throws SocketIOProtocolException
    {
        assert (raw != null);

        try
        {
            int type = raw.read();
            if(type == -1)
                throw new SocketIOProtocolException("Empty binary object received");

            return new EngineIOPacket(EngineIOPacket.Type.fromInt(type), raw);
        }
        catch (IOException e)
        {
            throw new SocketIOProtocolException("Cannot read packet type from binary object");
        }
    }

    public static EngineIOPacket createHandshakePacket(String session_id,
                                                       String[] upgrades,
                                                       long ping_interval,
                                                       long ping_timeout)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("sid", session_id);
        map.put("upgrades", upgrades);
        map.put("pingInterval", ping_interval);
        map.put("pingTimeout", ping_timeout);

        try
        {
            return new EngineIOPacket(EngineIOPacket.Type.OPEN, SocketIOProtocol.toJSON(map));
        }
        catch (SocketIOProtocolException e)
        {
            // ignore. never happen
            return null;
        }
    }

    public static EngineIOPacket createOpenPacket()
    {
        return new EngineIOPacket(EngineIOPacket.Type.OPEN);
    }

    public static EngineIOPacket createPingPacket(String data)
    {
        return new EngineIOPacket(EngineIOPacket.Type.PING, data);
    }

    public static EngineIOPacket createPongPacket(String data)
    {
        return new EngineIOPacket(EngineIOPacket.Type.PONG, data);
    }

    public static EngineIOPacket createMessagePacket(String data)
    {
        return new EngineIOPacket(EngineIOPacket.Type.MESSAGE, data);
    }
}
