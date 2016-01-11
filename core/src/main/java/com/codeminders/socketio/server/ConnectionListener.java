package com.codeminders.socketio.server;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public interface ConnectionListener
{
    /**
     * Callback to be called on new connection
     *
     * @param socket new socket
     * @throws ConnectionException thrown if caller want to fail the connection
     *                             for some reason (e.g. authentication error).
     *                             The error message will be sent to the client
     */
    void onConnect(Socket socket) throws ConnectionException;
}
