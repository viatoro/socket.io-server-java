package com.codeminders.socketio.server;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */
public class UnsupportedTransportException extends Exception {

    public UnsupportedTransportException(String name) {
        super("Unsupported transport " + name);
    }
}
