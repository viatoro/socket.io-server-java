package com.codeminders.socketio.server;

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class Room implements Outbound
{
    private String id;
    private List<Socket> sockets = Collections.synchronizedList(new LinkedList<Socket>());

    Room(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public void emit(String name, Object... args) throws SocketIOException
    {
        for(Socket s : sockets)
        {
            try
            {
                s.emit(name, args);
            }
            catch (SocketIOException e)
            {
                // ignore for now
                // TODO: add getLastError method?
            }
        }
    }

    public void join(Socket socket)
    {
        sockets.add(socket);
    }

    public void leave(Socket socket)
    {
        sockets.remove(socket);
    }

    public boolean contains(Socket socket)
    {
        return sockets.contains(socket);
    }
}
