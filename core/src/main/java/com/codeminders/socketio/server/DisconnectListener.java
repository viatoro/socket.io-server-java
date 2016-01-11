package com.codeminders.socketio.server;

import com.codeminders.socketio.common.DisconnectReason;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public interface DisconnectListener
{
    void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage);
}
