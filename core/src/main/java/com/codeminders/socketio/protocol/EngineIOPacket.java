package com.codeminders.socketio.protocol;

import java.io.InputStream;

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
        UNKNOWN(-1);

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

    private Type        type;
    private String      textData = "";
    private InputStream binaryData;

    public Type getType()
    {
        return type;
    }

    public String getTextData()
    {
        return textData;
    }

    public InputStream getBinaryData()
    {
        return binaryData;
    }

    public EngineIOPacket(Type type, String data)
    {
        this.type = type;
        this.textData = data;
    }

    public EngineIOPacket(Type type, InputStream binaryData)
    {
        this.type = type;
        this.textData = "";
        this.binaryData = binaryData;
    }

    public EngineIOPacket(Type type)
    {
        this(type, "");
    }
}
