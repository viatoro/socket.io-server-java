package com.codeminders.socketio.server;

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

import java.util.*;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class Socket implements Outbound, Inbound, DisconnectListener, EventListener
{
    private List<DisconnectListener>   disconnectListeners = new LinkedList<>();
    private Map<String, EventListener> eventListeners      = new LinkedHashMap<>();

    private Session   session; // Socket is Session + Namespace
    private Namespace namespace; //TODO: do we need Namespace object here or id is enough? or do we need it at all?

    public Socket(Session session, Namespace namespace)
    {
        this.session   = session;
        this.namespace = namespace;
    }

    public Namespace getNamespace()
    {
        return namespace;
    }

    /**
     * Set listener for an event. Only one listener per event is allowed.
     *
     * @param eventName event name
     * @param listener event listener
     */
    @Override
    public void on(String eventName, EventListener listener)
    {
        eventListeners.put(eventName, listener);
    }

    @Override
    public void disconnect()
    {
        //TODO: implement
        //TODO: drop whole session or only disconnect from the namespace?
        //TODO: for now will be dropping the whole session and underlying connection
    }

    @Override
    public void emit(String name, Object... args) throws SocketIOException
    {
        //TODO: pass namespace
        getSession().getConnection().emit(name, args);
    }

    /**
     * Add disconnect listener
     *
     * @param listener disconnect listener
     */
    public void on(DisconnectListener listener)
    {
        disconnectListeners.add(listener);
    }

    public Session getSession()
    {
        return session;
    }

    @Override
    public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage)
    {
        for (DisconnectListener listener : disconnectListeners)
            listener.onDisconnect(socket, reason, errorMessage);
    }

    @Override
    public Object onEvent(String name, Object[] args)
    {
        EventListener listener = eventListeners.get(name);
        if(listener == null)
            return null;

        return listener.onEvent(name, args);
    }
}
