package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public interface ACKListener
{
    void onACK(Object[] args);
}
