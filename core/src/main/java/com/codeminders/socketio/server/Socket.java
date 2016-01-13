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
    private String namespace;

    public Socket(Session session, String namespace)
    {
        this.session   = session;
        this.namespace = namespace;
    }

    public String getNamespace()
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

    /**
     * Drops whole session by disconnecting underlying transport
     */
    @Override
    public void disconnect(boolean closeConnection)
    {
        getSession().getConnection().disconnect(getNamespace(), closeConnection);
    }

    @Override
    public void emit(String name, Object... args) throws SocketIOException
    {
        getSession().getConnection().emit(getNamespace(), name, args);
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
