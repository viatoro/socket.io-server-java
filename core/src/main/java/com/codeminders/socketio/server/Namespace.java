package com.codeminders.socketio.server;

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class Namespace implements Outbound, ConnectionListener, DisconnectListener
{
    private String                   id;

    private List<Socket>             sockets             = Collections.synchronizedList(new LinkedList<Socket>());
    private List<ConnectionListener> connectionListeners = Collections.synchronizedList(new LinkedList<ConnectionListener>());

    Namespace(String id)
    {
        this.id = id;
    }

    @Override
    public void disconnect(boolean closeConnection)
    {
        for(Socket s : sockets)
            s.disconnect(closeConnection);
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
        Socket socket = new Socket(session, this.getId());
        socket.on(this);
        sockets.add(socket);

        return socket;
    }

    @Override
    public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage)
    {
        sockets.remove(socket);
    }
}
