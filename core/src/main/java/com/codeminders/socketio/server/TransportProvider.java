package com.codeminders.socketio.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import java.util.Collection;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */
public interface TransportProvider {

    /*
        Creates all the transports
     */
    void init(ServletConfig config);
    void destroy();

    /*
        Finds appropriate Transport class based on the rules defined at
        https://github.com/socketio/engine.io-protocol#transports
     */
    Transport getTransport(ServletRequest request)
            throws UnsupportedTransportException, SocketIOProtocolException;

    Transport getTransport(TransportType type);
    Collection<Transport> getTransports();
}
