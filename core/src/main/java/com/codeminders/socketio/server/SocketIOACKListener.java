package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public interface SocketIOACKListener
{
    void onACK(Object[] args);
}
