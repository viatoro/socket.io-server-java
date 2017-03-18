package com.codeminders.socketio.server.onemore;

import com.codeminders.socketio.server.SocketIOManager;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by asolod on 17.03.17.
 */
public abstract class WebsocketIOConnector implements ServerApplicationConfig {

    protected SocketIOManager socketIOManager = new SocketIOManager();

    protected abstract String getWebsocketURL();

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> set) {
        return new HashSet<ServerEndpointConfig>() {
            {
                add(ServerEndpointConfig.Builder
                        .create(WebsocketTransportConnection.class, getWebsocketURL())
                        .configurator(new ServerEndpointConfig.Configurator(){

                            /**
                             * This method is called by the container each time a new client connects to the logical endpoint this configurator configures.
                             */
                            @Override
                            public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {

                                // TODO: we have to lookup appropriate instance
                                // this instance we can lookup by sessionId
                                // but at this point we do not have access to session
                                return (T) new WebsocketTransportConnection();
                            }


                        })
                        .build());
            }
        };
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> set) {
        return Collections.emptySet();
    }


}