package com.codeminders.socketio.protocol;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class PlainACKPacket extends ACKPacket
{
    public PlainACKPacket(int id, Object[] args)
    {
        super(Type.ACK, id, args);
    }


    @Override
    public String getPrefix()
    {
        return "";
    }
}
