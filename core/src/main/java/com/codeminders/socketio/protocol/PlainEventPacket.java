package com.codeminders.socketio.protocol;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
//TODO: move to SocketIOProtocol
public class PlainEventPacket extends EventPacket
{
    public PlainEventPacket(int id, String ns, String name, Object[] args)
    {
        super(Type.EVENT, id, ns, name, args);
    }
}
