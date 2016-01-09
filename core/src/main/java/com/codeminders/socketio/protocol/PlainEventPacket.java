package com.codeminders.socketio.protocol;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class PlainEventPacket extends EventPacket
{
    public PlainEventPacket(int id, String name, Object[] args)
    {
        super(Type.EVENT, id, name, args);
    }

    @Override
    protected String getPrefix()
    {
        return "";
    }
}
