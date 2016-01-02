package com.codeminders.socketio.protocol;

import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.util.JSON;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Implementation of Socket.IO Protocol version 4
 *
 * @author Alexander Sova (bird@codeminders.com)
 */
public final class SocketIOProtocol
{
    private static class EmptyPacket extends SocketIOPacket
    {
        public EmptyPacket(Type type)
        {
            super(type);
        }

        @Override
        protected String getData()
        {
            return "";
        }
    }

    private SocketIOProtocol()
    {
    }

    public static String encode(SocketIOPacket packet)
    {
        return String.valueOf(packet.getType().value()) + packet.getData();
    }

    public static SocketIOPacket decode(String raw)
            throws SocketIOProtocolException
    {
        assert (raw != null);

        if (raw.length() < 1)
            throw new SocketIOProtocolException("Empty SocketIO packet");

        try
        {
            SocketIOPacket.Type type = SocketIOPacket.Type.fromInt(Integer.parseInt(raw.substring(0, 1)));
            String data = raw.substring(1);

            switch (type)
            {
                case CONNECT:
                    return createConnectPacket();
                case DISCONNECT:
                    return createDisconnectPacket();
                case EVENT:
                    //TODO: move this logic to packet.decode()?
                    ParsePosition pos = new ParsePosition(0);
                    Number id = extractId(data, pos);
                    if (id == null)
                        id = -1;
                    data = data.substring(pos.getIndex());
                    Object json = JSON.parse(data);
                    if (!(json instanceof Object[]) || ((Object[]) json).length == 0)
                        throw new SocketIOProtocolException("Invalid JSON in EVENT message packet: " + data);

                    Object[] args = (Object[]) json;
                    if (!(args[0] instanceof String))
                        throw new SocketIOProtocolException("Invalid JSON in EVENT message packet. First argument must be string: " + data);

                    return new SocketIOEventPacket(id.intValue(),
                            args[0].toString(), Arrays.copyOfRange(args, 1, args.length));

                default:
                    throw new SocketIOProtocolException("Unsupported packet type");
            }
        }
        catch (NumberFormatException | SocketIOProtocolException e)
        {
            throw new SocketIOProtocolException("Invalid EIO packet type: " + raw);
        }
    }

    public static SocketIOPacket createEventPacket(String name, Object[] args)
    {
        return new SocketIOEventPacket(name, args);
    }

    public static SocketIOPacket createACKPacket(int id, Object args)
    {
        return new SocketIOACKPacket(id, args);
    }

    public static SocketIOPacket createDisconnectPacket()
    {
        return new EmptyPacket(SocketIOPacket.Type.DISCONNECT);
    }

    public static SocketIOPacket createConnectPacket()
    {
        return new EmptyPacket(SocketIOPacket.Type.CONNECT);
    }

    private static Number extractId(String str, ParsePosition pos)
    {
        return new DecimalFormat("#").parse(str, pos);
    }
}
