package com.codeminders.socketio.protocol;

import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.util.JSON;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of Socket.IO Protocol version 4
 *
 * @author Alexander Sova (bird@codeminders.com)
 */
public final class SocketIOProtocol
{
    private static final Logger LOGGER = Logger.getLogger(SocketIOProtocol.class.getName());

    public static final String DEFAULT_NAMESPACE = "/";

    static final String ATTACHMENTS_DELIMITER = "-";
    static final String NAMESPACE_PREFIX      = "/";
    static final String NAMESPACE_DELIMITER   = ",";

    public static SocketIOPacket createErrorPacket(String namespace, final Object args)
    {
        return new SocketIOPacket(SocketIOPacket.Type.ERROR, namespace)
        {
            @Override
            protected String encodeArgs()
            {
                return JSON.toString(args);
            }
        };
    }

    public static String encodeAttachments(int size)
    {
        return String.valueOf(size) + ATTACHMENTS_DELIMITER;
    }

    private static class EmptyPacket extends SocketIOPacket
    {
        public EmptyPacket(Type type, String ns)
        {
            super(type, ns);
        }

        @Override
        protected String encodeArgs()
        {
            return "";
        }
    }

    private SocketIOProtocol()
    {
    }

    public static SocketIOPacket decode(String data)
            throws SocketIOProtocolException
    {
        assert (data != null);

        if (data.length() < 1)
            throw new SocketIOProtocolException("Empty SIO packet");

        try
        {
            ParsePosition pos = new ParsePosition(0);
            SocketIOPacket.Type type = decodePacketType(data, pos);

            int attachments = 0;
            if (type == SocketIOPacket.Type.BINARY_ACK || type == SocketIOPacket.Type.BINARY_EVENT)
                attachments = decodeAttachments(data, pos);

            String ns = decodeNamespace(data, pos);
            int packet_id = decodePacketId(data, pos);
            Object json = decodeArgs(data, pos);

            Object[] args = null;
            String eventName = "";
            if (type == SocketIOPacket.Type.EVENT || type == SocketIOPacket.Type.BINARY_EVENT)
            {
                if (!(json instanceof Object[]))
                    throw new SocketIOProtocolException("Array payload is expected");

                args = (Object[]) json;
                if (args.length == 0)
                    throw new SocketIOProtocolException("Missing event name");

                eventName = args[0].toString();
                args = Arrays.copyOfRange(args, 1, args.length);
            }

            if (type == SocketIOPacket.Type.ACK || type == SocketIOPacket.Type.BINARY_ACK)
            {
                if (!(json instanceof Object[]))
                    throw new SocketIOProtocolException("Array payload is expected");

                args = (Object[]) json;
            }

            switch (type)
            {
                case CONNECT:
                    return createConnectPacket(ns);

                case DISCONNECT:
                    return createDisconnectPacket(ns);

                case EVENT:
                    return new PlainEventPacket(packet_id, ns, eventName, args);

                case ACK:
                    return new PlainACKPacket(packet_id, ns, args);

                case ERROR:
                    return createErrorPacket(ns, json);

                case BINARY_EVENT:
                    return new BinaryEventPacket(packet_id, ns, eventName, args, attachments);

                case BINARY_ACK:
                    return new BinaryACKPacket(packet_id, ns, args, attachments);

                default:
                    throw new SocketIOProtocolException("Unsupported packet type " + type);
            }
        }
        catch (NumberFormatException | SocketIOProtocolException e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Invalid SIO packet: " + data, e);

            throw new SocketIOProtocolException("Invalid SIO packet: " + data);
        }
    }

    /**
     * This method could create either EventPacket or BinaryEventPacket based
     * on the content of args parameter.
     * If args has any InputStream inside then SockeIOBinaryEventPacket will be created
     */
    public static SocketIOPacket createEventPacket(int packet_id, String ns, String name, Object[] args)
    {
        if (hasBinary(args))
            return new BinaryEventPacket(packet_id, ns, name, args);
        else
            return new PlainEventPacket(packet_id, ns, name, args);
    }

    public static SocketIOPacket createACKPacket(int id, String ns, Object[] args)
    {
        if (hasBinary(args))
            return new BinaryACKPacket(id, ns, args);
        else
            return new PlainACKPacket(id, ns, args);
    }

    public static SocketIOPacket createDisconnectPacket(String ns)
    {
        return new EmptyPacket(SocketIOPacket.Type.DISCONNECT, ns);
    }

    public static SocketIOPacket createConnectPacket(String ns)
    {
        return new EmptyPacket(SocketIOPacket.Type.CONNECT, ns);
    }

    static String decodeNamespace(String data, ParsePosition pos)
    {
        String ns = DEFAULT_NAMESPACE;
        if (data.startsWith(NAMESPACE_PREFIX, pos.getIndex()))
        {
            int idx = data.indexOf(NAMESPACE_DELIMITER, pos.getIndex());
            if (idx < 0)
            {
                ns = data.substring(pos.getIndex());
                pos.setIndex(data.length());
            }
            else
            {
                ns = data.substring(pos.getIndex(), idx);
                pos.setIndex(idx + 1);
            }
        }
        return ns;
    }

    static int decodeAttachments(String data, ParsePosition pos)
            throws SocketIOProtocolException
    {
        Number n = new DecimalFormat("#").parse(data, pos);
        if (n == null || n.intValue() == 0)
            throw new SocketIOProtocolException("No attachments defined in BINARY packet: " + data);

        pos.setIndex(pos.getIndex() + 1); //skipping '-' delimiter

        return n.intValue();
    }

    static int decodePacketId(String data, ParsePosition pos)
    {
        Number id = new DecimalFormat("#").parse(data, pos);
        if (id == null)
            return -1;

        return id.intValue();
    }

    static SocketIOPacket.Type decodePacketType(String data, ParsePosition pos)
            throws SocketIOProtocolException
    {
        int idx = pos.getIndex();
        SocketIOPacket.Type type = SocketIOPacket.Type.fromInt(Integer.parseInt(data.substring(idx, idx + 1)));
        pos.setIndex(idx + 1);
        return type;
    }

    static Object decodeArgs(String data, ParsePosition pos)
            throws SocketIOProtocolException
    {
        Object json = JSON.parse(data.substring(pos.getIndex()));
        pos.setIndex(data.length());
        return json;
    }

    static SocketIOPacket decodeEventOrACK(SocketIOPacket.Type type, String ns, String data)
            throws SocketIOProtocolException
    {
        int attachments = 0;
        ParsePosition pos = new ParsePosition(0);
        if (type == SocketIOPacket.Type.BINARY_EVENT || type == SocketIOPacket.Type.BINARY_ACK)
        {
            Number n = new DecimalFormat("#").parse(data, pos);
            if (n == null || n.intValue() == 0)
                throw new SocketIOProtocolException("No attachments defined in BINARY packet: " + data);

            attachments = n.intValue();

            pos.setIndex(pos.getIndex() + 1); //skipping '-' delimiter
        }

        Number id = new DecimalFormat("#").parse(data, pos);
        if (id == null)
            id = -1;
        data = data.substring(pos.getIndex());

        Object json = JSON.parse(data);

        // EVENT or ACK arguments are always passed as array
        if (!(json instanceof Object[]) || ((Object[]) json).length == 0)
            throw new SocketIOProtocolException("Invalid JSON in the EVENT or ACK packet: " + data);

        Object[] args = (Object[]) json;
        if (type == SocketIOPacket.Type.ACK)
            return new PlainACKPacket(id.intValue(), ns, args);

        if (type == SocketIOPacket.Type.BINARY_ACK)
            return new BinaryACKPacket(id.intValue(), ns, args, attachments);

        // the first argument for EVENT is always event's name
        if (!(args[0] instanceof String))
            throw new SocketIOProtocolException("Invalid JSON in the EVENT packet. First argument must be string: " + data);

        String name = args[0].toString();
        args = Arrays.copyOfRange(args, 1, args.length);

        if (type == SocketIOPacket.Type.BINARY_EVENT)
            return new BinaryEventPacket(id.intValue(), ns, name, args, attachments);
        else
            return new PlainEventPacket(id.intValue(), ns, name, args);
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
        else if (json instanceof InputStream)
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
     * @param packet     packet to add a binary object
     * @param attachment binary object to insert
     */
    public static void insertBinaryObject(BinaryPacket packet, InputStream attachment)
            throws IllegalArgumentException
    {
        packet.setArgs((Object[]) insertBinaryObject(packet.getArgs(), attachment));
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
            Map<Object, Object> map = (Map) json;

            if (isPlaceholder(map))
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