package com.codeminders.socketio.protocol;

import com.codeminders.socketio.util.JSON;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class ACKPacket extends SocketIOPacket
{
    private int id;
    private Object[] args;

    public ACKPacket(Type type, int id, Object[] args)
    {
        super(type);
        this.id = id;
        this.args = args;
    }

    @Override
    protected String getData()
    {
        return getPrefix() + String.valueOf(id) + JSON.toString(args);
    }

    public Object[] getArgs()
    {
        return args;
    }

    public int getId()
    {
        return id;
    }

    public void setArgs(Object[] args)
    {
        this.args = args;
    }

    protected abstract String getPrefix();
}
