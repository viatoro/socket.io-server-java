package com.codeminders.socketio.protocol;

import com.codeminders.socketio.server.SocketIOProtocolException;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class SocketIOPacket
{
    public enum Type
    {
        CONNECT(0),
        DISCONNECT(1),
        EVENT(2),
        ACK(3),
        ERROR(4),
        BINARY_EVENT(5),
        BINARY_ACK(6);

        private int value;

        Type(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }

        public static Type fromInt(int i) throws SocketIOProtocolException
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
                    throw new SocketIOProtocolException("Unexpected packet type: " + i);
            }
        }
    }

    private Type   type;
    private String namespace;

    public Type getType()
    {
        return type;
    }

    protected abstract String getData();

    public String getNamespace()
    {
        return namespace;
    }

    SocketIOPacket(Type type)
    {
        this(type, "/");
    }

    SocketIOPacket(Type type, String namespace)
    {
        this.type = type;
        this.namespace = namespace;
    }

    public String encode()
    {
        return String.valueOf(getType().value()) + getData();
    }

}
