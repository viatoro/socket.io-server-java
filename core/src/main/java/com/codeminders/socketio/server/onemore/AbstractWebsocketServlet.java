package com.codeminders.socketio.server.onemore;

import com.codeminders.socketio.server.SocketIOServlet;
import com.codeminders.socketio.server.TransportProvider;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by asolod on 18.03.17.
 */
public abstract class AbstractWebsocketServlet extends SocketIOServlet implements ServerApplicationConfig {

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        TransportProvider transportProvider = new WebsocketTransportProvider();
        transportProvider.init(config, getServletContext());
        setTransportProvider(transportProvider);
    }

    protected abstract String getWebsocketURL();

   @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> set) {
        return new HashSet<ServerEndpointConfig>(Arrays.asList((ServerEndpointConfig.Builder
                        .create(WebsocketEndPoint.class, "/socket.io/")
                        .configurator(new ServerEndpointConfig.Configurator(){
                            @Override
                            public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
                                super.modifyHandshake(sec, request, response);
                            }
                        })
                        .build())));
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> set) {
        return Collections.emptySet();
    }

}
