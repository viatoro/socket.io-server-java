package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class SocketIOProtocolException extends Exception
{

    public SocketIOProtocolException(String message)
    {
        super(message);
    }

    public SocketIOProtocolException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
