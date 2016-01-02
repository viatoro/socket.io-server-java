package com.codeminders.socketio.protocol;

import com.codeminders.socketio.util.JSON;


/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class SocketIOACKPacket extends SocketIOPacket
{
    private int id;
    private Object args;

    public SocketIOACKPacket(int id, Object args)
    {
        super(Type.ACK);
        this.id = id;
        this.args = args;
    }

    @Override
    protected String getData()
    {
        return String.valueOf(id) + JSON.toString(new Object[] { args });
    }
}
