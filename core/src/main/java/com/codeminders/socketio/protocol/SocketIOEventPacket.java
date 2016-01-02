package com.codeminders.socketio.protocol;

import com.codeminders.socketio.util.JSON;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class SocketIOEventPacket extends SocketIOPacket
{
    private int      id;
    private String   name;
    private Object[] args;

    public SocketIOEventPacket(int id, String name, Object... args)
    {
        super(Type.EVENT);
        this.id = id;
        this.name = name;
        this.args = args;
    }

    public SocketIOEventPacket(String name, Object... args)
    {
        super(Type.EVENT);
        this.id = -1;
        this.name = name;
        this.args = args;
    }

    @Override
    protected String getData()
    {
        String str = "";
        if(id > 0)
            str = String.valueOf(id);

        ArrayList<Object> data = new ArrayList<>();
        data.add(name);
        data.addAll(Arrays.asList(args));

        return str + JSON.toString(data.toArray());
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
}
