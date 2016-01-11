package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public interface EventListener
{
    //TODO: add boolean parameter to indicate that ACK is requested by the client
    Object onEvent(String name, Object[] args);
}
