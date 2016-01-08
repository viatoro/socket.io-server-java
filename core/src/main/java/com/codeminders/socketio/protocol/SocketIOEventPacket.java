package com.codeminders.socketio.protocol;

import com.codeminders.socketio.util.JSON;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class SocketIOEventPacket extends SocketIOPacket
{
    private int      id;
    private String   name;
    private Object[] args;

    protected SocketIOEventPacket(Type type, int id, String name, Object[] args)
    {
        super(type);
        this.id = id;
        this.name = name;
        this.args = args;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public Object[] getArgs()
    {
        return args;
    }

    public void setArgs(Object[] args)
    {
        this.args = args;
    }

    @Override
    protected String getData()
    {
        String str = getPrefix();

        // packet id to request ACK
        if(id >= 0)
            str += String.valueOf(id);

        // adding name of the event as a first argument
        ArrayList<Object> data = new ArrayList<>();
        data.add(getName());
        data.addAll(Arrays.asList(getArgs()));

        return str + JSON.toString(data.toArray());
    }

    protected abstract String getPrefix();
}
