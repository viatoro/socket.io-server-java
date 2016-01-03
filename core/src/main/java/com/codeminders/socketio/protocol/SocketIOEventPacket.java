package com.codeminders.socketio.protocol;

import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.util.JSON;

import java.text.DecimalFormat;
import java.text.ParsePosition;
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

    static SocketIOEventPacket decode(String data)
            throws SocketIOProtocolException
    {
        ParsePosition pos = new ParsePosition(0);
        Number id = new DecimalFormat("#").parse(data, pos);
        if (id == null)
            id = -1;
        data = data.substring(pos.getIndex());
        Object json = JSON.parse(data);
        if (!(json instanceof Object[]) || ((Object[]) json).length == 0)
            throw new SocketIOProtocolException("Invalid JSON in EVENT message packet: " + data);

        Object[] args = (Object[]) json;
        if (!(args[0] instanceof String))
            throw new SocketIOProtocolException("Invalid JSON in EVENT message packet. First argument must be string: " + data);

        return new SocketIOEventPacket(id.intValue(),
                args[0].toString(), Arrays.copyOfRange(args, 1, args.length));
    }
}
