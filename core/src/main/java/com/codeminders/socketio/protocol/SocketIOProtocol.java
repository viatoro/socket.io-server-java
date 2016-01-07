package com.codeminders.socketio.protocol;

import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.util.JSON;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.*;

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
                case BINARY_EVENT:
                    return decodeEvent(type, data);

                default:
                    throw new SocketIOProtocolException("Unsupported packet type");
            }
        }
        catch (NumberFormatException | SocketIOProtocolException e)
        {
            throw new SocketIOProtocolException("Invalid EIO packet type: " + raw);
        }
    }

    /**
     * This method could create either SocketIOEventPacket or SocketIOBinaryEventPacket based
     * on the content of args parameter.
     * If args has any InputStream inside then SockeIOBinaryEventPacket will be created
     */
    public static SocketIOPacket createEventPacket(String name, Object[] args)
    {
        if (hasBinary(args))
            return new SocketIOBinaryEventPacket(name, args);
        else
            return new SocketIOPlainEventPacket(name, args);
    }

    public static SocketIOPacket createACKPacket(int id, Object args)
    {
        //TODO: check for binary and create BINARY_ACK if needed
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

    static SocketIOEventPacket decodeEvent(SocketIOPacket.Type type, String data)
            throws SocketIOProtocolException
    {
        int attachments = 0;
        ParsePosition pos = new ParsePosition(0);
        if (type == SocketIOPacket.Type.BINARY_EVENT)
        {
            Number n = new DecimalFormat("#").parse(data, pos);
            if (n == null || n.intValue() == 0)
                throw new SocketIOProtocolException("No attachments defined in BINARY_EVENT packet: " + data);

            attachments = n.intValue();

            pos.setIndex(pos.getIndex() + 1); //skipping '-' delimiter
        }

        Number id = new DecimalFormat("#").parse(data, pos);
        if (id == null)
            id = -1;
        data = data.substring(pos.getIndex());
        Object json = JSON.parse(data);
        if (!(json instanceof Object[]) || ((Object[]) json).length == 0)
            throw new SocketIOProtocolException("Invalid JSON in EVENT message packet: " + data);

        Object[] args = (Object[]) json;
        if (!(args[0] instanceof String))
            throw new SocketIOProtocolException("Invalid JSON in EVENT message packet. First argument must be string: " + data);

        if (type == SocketIOPacket.Type.BINARY_EVENT)
            return new SocketIOBinaryEventPacket(id.intValue(),
                    args[0].toString(), Arrays.copyOfRange(args, 1, args.length), attachments);
        else
            return new SocketIOPlainEventPacket(id.intValue(),
                    args[0].toString(), Arrays.copyOfRange(args, 1, args.length));
    }

    private static boolean hasBinary(Object args)
    {
        if (args.getClass().isArray())
        {
            for (Object o : (Object[]) args)
                if (hasBinary(o))
                    return true;
        }
        else if (args instanceof Map)
        {
            for (Object o : ((Map) args).values())
                if (hasBinary(o))
                    return true;
        }
        else
        {
            return (args instanceof InputStream);
        }

        return false;
    }

    /**
     * Extracts binary objects (InputStream) from JSON and replaces it with
     * placeholder objects {@code {"_placeholder":true,"num":1} }
     * This method to be used before sending the packet
     *
     * @param json        JSON object
     * @param attachments container for extracted binary object
     * @return modified JSON object
     */
    @SuppressWarnings("unchecked")
    static Object extractBinaryObjects(Object json, List<InputStream> attachments)
    {
        //TODO: what about Collection? for now only array is supported
        if (json.getClass().isArray())
        {
            ArrayList<Object> array = new ArrayList<>(((Object[]) json).length);

            for (Object o : (Object[]) json)
                array.add(extractBinaryObjects(o, attachments));

            return array.toArray();
        }
        else if (json instanceof Map)
        {
            Map<Object, Object> map = new LinkedHashMap<>();
            Set<Map.Entry> entries = ((Map) json).entrySet();

            for (Map.Entry e : entries)
                map.put(e.getKey(), extractBinaryObjects(e, attachments));

            return map;
        }
        else
        if (json instanceof InputStream)
        {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("_placeholder", true);
            map.put("num", attachments.size());
            attachments.add((InputStream) json);

            return map;
        }
        else
            return json;
    }

    /**
     * Looks for the first placeholder objects in {@code json.getArgs() } {@code {"_placeholder":true,"num":1}} and
     * replaces it with {@code attachment}
     * This method to be used when all expected binary objects are received
     *
     * @param packet      packet to add a binary object
     * @param attachment  binary object to insert
     */
    public static void insertBinaryObject(SocketIOBinaryEventPacket packet, InputStream attachment)
        throws IllegalArgumentException
    {
        packet.setArgs((Object[])insertBinaryObject(packet.getArgs(), attachment));
    }

    //TODO: report if no placeholder was found
    /**
     * This method makes copy of {@code json} replacing first found placeholder entry with {@code attachment}
     * Ignoring "num" parameter for now.
     *
     * @param json       JSON object
     * @param attachment InputStream object to insert
     * @return copy of JSON object
     */
    @SuppressWarnings("unchecked")
    public static Object insertBinaryObject(Object json, InputStream attachment)
    {
        //TODO: what about Collection? for now only array is supported
        if (json.getClass().isArray())
        {
            ArrayList<Object> copy = new ArrayList<>(((Object[]) json).length);

            for (Object o : (Object[]) json)
                copy.add(insertBinaryObject(o, attachment));

            return copy.toArray();
        }
        else if (json instanceof Map)
        {
            Map<Object, Object> map = (Map)json;

            if(isPlaceholder(map))
                return attachment;

            Map<Object, Object> copy = new LinkedHashMap<>();
            Set<Map.Entry<Object, Object>> entries = map.entrySet();

            for (Map.Entry e : entries)
                copy.put(e.getKey(), insertBinaryObject(e.getValue(), attachment));

            return copy;
        }
        else
            return json;

    }

    private static boolean isPlaceholder(Map<Object, Object> map)
    {
        //TODO: check map.size() == 2 && map.get("num") instanceof Integer
        return Boolean.TRUE.equals(map.get("_placeholder"));
    }
}
