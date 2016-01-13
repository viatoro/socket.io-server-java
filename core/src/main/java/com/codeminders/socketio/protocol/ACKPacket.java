package com.codeminders.socketio.protocol;

import com.codeminders.socketio.util.JSON;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class ACKPacket extends SocketIOPacket
{
    private Object[] args;

    public ACKPacket(Type type, int id, String ns, Object[] args)
    {
        super(type, id, ns);
        this.args = args;
    }

    @Override
    protected String encodeArgs()
    {
        return JSON.toString(args);
    }

    public Object[] getArgs()
    {
        return args;
    }

    public void setArgs(Object[] args)
    {
        this.args = args;
    }

}
