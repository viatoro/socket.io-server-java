package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class EngineIOPacket
{
    public enum Type
    {
        OPEN(0),
        CLOSE(1),
        PING(2),
        PONG(3),
        MESSAGE(4),
        UPGRADE(5),
        NOOP(6),
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
                case 0: return OPEN;
                case 1: return CLOSE;
                case 2: return PING;
                case 3: return PONG;
                case 4: return MESSAGE;
                case 5: return UPGRADE;
                case 6: return NOOP;
                default:
                    return UNKNOWN;
            }
        }
    }

    private Type   type;
    private String data;

    public Type getType()
    {
        return type;
    }

    public String getData()
    {
        return data;
    }

    public EngineIOPacket(Type type, String data)
    {
        this.type = type;
        this.data = data;
    }

    public EngineIOPacket(Type type)
    {
        this(type, "");
    }
}
