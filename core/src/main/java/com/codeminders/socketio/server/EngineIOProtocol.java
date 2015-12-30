package com.codeminders.socketio.server;

import com.codeminders.socketio.util.JSON;

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
        return String.valueOf(packet.getType().value()) + packet.getData();
    }

    //TODO: do we need separate exception for EIO protocol?
    public static EngineIOPacket decode(String raw)
            throws SocketIOProtocolException
    {
        assert (raw != null);

        if(raw.length() < 1)
            throw new SocketIOProtocolException("Empty EIO packet");

        try
        {
            return new EngineIOPacket(
                    EngineIOPacket.Type.fromInt(Integer.parseUnsignedInt(raw.substring(0, 1))),
                    raw.substring(1));
        }
        catch (NumberFormatException e)
        {
            throw new SocketIOProtocolException("Invalid EIO packet type: " + raw);
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
        return new EngineIOPacket(EngineIOPacket.Type.OPEN, JSON.toString(map));
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
