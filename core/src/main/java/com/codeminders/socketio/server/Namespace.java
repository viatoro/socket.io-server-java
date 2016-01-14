package com.codeminders.socketio.server;

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

import java.util.*;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class Namespace implements Outbound, ConnectionListener, DisconnectListener
{
    private String                   id;

    private List<Socket>             sockets             = Collections.synchronizedList(new LinkedList<Socket>());
    private List<ConnectionListener> connectionListeners = Collections.synchronizedList(new LinkedList<ConnectionListener>());
    private Map<String, Room>        rooms               = Collections.synchronizedMap(new LinkedHashMap<String, Room>());

    Namespace(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public void emit(String name, Object... args)
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


    public void on(ConnectionListener listener)
    {
        connectionListeners.add(listener);
    }

    @Override
    public void onConnect(Socket socket)
            throws ConnectionException
    {
        for(ConnectionListener listener : connectionListeners)
            listener.onConnect(socket);
    }

    public Socket createSocket(Session session)
    {
        Socket socket = new Socket(session, this);
        socket.on(this);
        sockets.add(socket);

        return socket;
    }

    @Override
    public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage)
    {
        leaveAll(socket);
        sockets.remove(socket);
    }

    public Room in(String roomId)
    {
        Room room = rooms.get(roomId);
        if(room == null)
        {
            room = new Room(roomId);
            rooms.put(roomId, room);
        }
        return room;
    }

    void leaveAll(Socket socket)
    {
        for (Room room : rooms.values())
        {
            if(room.contains(socket))
                room.leave(socket);
        }
    }
}
