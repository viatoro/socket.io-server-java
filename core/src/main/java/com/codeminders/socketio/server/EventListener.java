package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public interface EventListener
{
    Object onEvent(String name, Object[] args);
}
