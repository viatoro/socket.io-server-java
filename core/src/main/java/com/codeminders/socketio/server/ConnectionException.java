package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class ConnectionException extends Exception
{
    private Object args;

    public Object getArgs()
    {
        return args;
    }

    public ConnectionException(String message, Throwable cause, Object args)
    {
        super(message, cause);
        this.args = args;
    }

    public ConnectionException(Throwable cause, Object args)
    {
        super(cause);
        this.args = args;
    }

    public ConnectionException(Object args)
    {
        this.args = args;
    }
}
