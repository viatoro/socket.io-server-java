package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class SocketIOPacket
{
    public enum Type
    {
        CONNECT(0),
        DISCONNECT(1),
        EVENT(2),
        ACK(3),
        ERROR(4),
        BINARY_EVENT(5),
        BINARY_ACK(6),
        UNKNOWN(Integer.MAX_VALUE);

        private int value;

        Type(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }

        public static Type fromInt(int i)
        {
            switch (i)
            {
                case 0: return CONNECT;
                case 1: return DISCONNECT;
                case 2: return EVENT;
                case 3: return ACK;
                case 4: return ERROR;
                case 5: return BINARY_EVENT;
                case 6: return BINARY_ACK;
                default:
                    return UNKNOWN;
            }
        }
    }

    private Type   type;
    private String data;
    private String namespace;

    public Type getType()
    {
        return type;
    }

    public String getData()
    {
        return data;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public SocketIOPacket(Type type)
    {
        this(type, "", "/");
    }

    public SocketIOPacket(Type type, String data)
    {
        this(type, data, "/");
    }

    public SocketIOPacket(Type type, String data, String namespace)
    {
        this.type = type;
        this.data = data;
        this.namespace = namespace;
    }
}
