package com.codeminders.socketio.protocol;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
//TODO: move to SocketIOProtocol
public class PlainACKPacket extends ACKPacket
{
    public PlainACKPacket(int id, String ns, Object[] args)
    {
        super(Type.ACK, id, ns, args);
    }
}
