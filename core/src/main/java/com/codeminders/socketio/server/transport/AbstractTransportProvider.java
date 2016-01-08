package com.codeminders.socketio.server.transport;

import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.server.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class AbstractTransportProvider implements TransportProvider {

    private static final Logger LOGGER = Logger.getLogger(AbstractTransportProvider.class.getName());

    protected Map<TransportType, Transport> transports = new EnumMap<>(TransportType.class);

    /**
     *   Creates and initializes all available transports
     */
    @Override
    public void init(ServletConfig config)
    {
        addIfNotNull(TransportType.XHR_POLLING,   createXHTPollingTransport());
        addIfNotNull(TransportType.JSONP_POLLING, createJSONPPollingTransport());
        addIfNotNull(TransportType.XHR_POLLING,   createWebSocketTransport());

        for(Transport t : transports.values())
            t.init(config);
    }

    @Override
    public void destroy()
    {
        for (Transport t : getTransports())
            t.destroy();
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

        if("websocket".equals(transportName))
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

    private void addIfNotNull(TransportType type, Transport transport)
    {
        if(transport != null)
            transports.put(type, transport);
    }
}
