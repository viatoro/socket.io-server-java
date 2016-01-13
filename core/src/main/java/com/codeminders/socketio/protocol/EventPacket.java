package com.codeminders.socketio.protocol;

import com.codeminders.socketio.util.JSON;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class EventPacket extends SocketIOPacket
{
    private String   name;
    private Object[] args;

    protected EventPacket(Type type, int id, String ns, String name, Object[] args)
    {
        super(type, id, ns);
        this.name = name;
        this.args = args;
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
    protected String encodeArgs()
    {
        // adding name of the event as a first argument
        ArrayList<Object> data = new ArrayList<>();
        data.add(getName());
        data.addAll(Arrays.asList(getArgs()));

        return JSON.toString(data.toArray());
    }
}

