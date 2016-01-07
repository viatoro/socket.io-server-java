package com.codeminders.socketio.protocol;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class SocketIOPlainEventPacket extends SocketIOEventPacket
{
    public SocketIOPlainEventPacket(int id, String name, Object[] args)
    {
        super(Type.EVENT, id, name, args);
    }

    public SocketIOPlainEventPacket(String name, Object[] args)
    {
        super(Type.EVENT, -1, name, args);
    }

    protected Object[] encodeArgs()
    {
        return getArgs(); //do nothing
    }
}
