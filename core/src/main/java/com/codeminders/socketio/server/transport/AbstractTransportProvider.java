package com.codeminders.socketio.server.transport;

import com.codeminders.socketio.server.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alexander Sova <bird@codeminders.com>
 */
public abstract class AbstractTransportProvider implements TransportProvider {

    private static final Logger LOGGER = Logger.getLogger(AbstractTransportProvider.class.getName());

    protected Map<TransportType, Transport> transports = new HashMap<>();

    /**
     *   Creates and initializes all available transports
     */
    @Override
    public void init(ServletConfig config)
    {
        transports.put(TransportType.XHR_POLLING,   createXHTPollingTransport());
        transports.put(TransportType.JSONP_POLLING, createJSONPPollingTransport());

        //TODO: should we allow server to work whithout websocket support?
        //TODO: websocket is one of the "must support" transports for socket.io 1.0 servers
        Transport transport = createWebSocketTransport();
        if(transport != null)
        {
            transports.put(TransportType.WEB_SOCKET, transport);
            transport = createFlashSocketTransport(transport);
            if(transport != null)
                transports.put(TransportType.FLASH_SOCKET, transport);
        }

        for(Transport t : transports.values())
            t.init(config);
    }

    @Override
    public void destroy() {
        for (Transport t : getTransports()) {
            t.destroy();
        }
    }

    @Override
    public Transport getTransport(ServletRequest request)
            throws UnsupportedTransportException, SocketIOProtocolException
    {
        //TODO: check EIO version? I do not see it in the spec but it is in actual requests. Ignoring it for now.
        String transportName = request.getParameter(EngineIOProtocol.TRANSPORT);
        if(transportName == null)
            throw new SocketIOProtocolException("Missing transport parameter");

        TransportType type = TransportType.UNKNOWN;

        if("websocket".equals(transportName) || "flashsocket".equals(transportName))
            type = TransportType.from(transportName);

        if("polling".equals(transportName)) {
            if(request.getParameter(EngineIOProtocol.JSONP_INDEX) != null)
                type = TransportType.JSONP_POLLING;
            else
                type = TransportType.XHR_POLLING;
        }

        Transport t = transports.get(type);
        if(t == null)
            throw new UnsupportedTransportException(transportName);
        return t;
    }

    @Override
    public Transport getTransport(TransportType type)
    {
        return transports.get(type);
    }

    @Override
    public Collection<Transport> getTransports()
    {
        return transports.values();
    }

    protected Transport createFlashSocketTransport(Transport delegate)
    {
        return null;
    }

    protected Transport createXHTPollingTransport()
    {
        return null;
    }

    protected Transport createJSONPPollingTransport()
    {
        return null;
    }

    protected Transport createWebSocketTransport()
    {
        return null;
    }


}
