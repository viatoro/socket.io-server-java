package com.codeminders.socketio.protocol;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class SocketIOPlainACKPacket extends SocketIOACKPacket
{
    public SocketIOPlainACKPacket(int id, Object[] args)
    {
        super(Type.ACK, id, args);
    }


    @Override
    public String getPrefix()
    {
        return "";
    }
}
