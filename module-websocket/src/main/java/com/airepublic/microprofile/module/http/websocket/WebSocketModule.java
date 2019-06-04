package com.airepublic.microprofile.module.http.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.microprofile.config.Config;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.Reflections;
import com.airepublic.microprofile.core.ServerContext;
import com.airepublic.microprofile.core.pathmatcher.MappingResult;
import com.airepublic.microprofile.module.http.HttpModule;
import com.airepublic.microprofile.module.http.websocket.server.WsSci;
import com.airepublic.microprofile.module.http.websocket.server.WsServerContainer;

public class WebSocketModule extends HttpModule {
    private WsServerContainer webSocketContainer;


    @Override
    public String getName() {
        return "WebSocket";
    }


    @Override
    protected Class<? extends AbstractIOHandler> findMapping(final String path) {
        if (webSocketContainer != null) {
            final MappingResult<ServerEndpointConfig> result = webSocketContainer.getMapping().findMapping(path);

            if (result != null) {
                return WebSocketIOHandler.class;
            }
        }

        return null;
    }


    @Override
    public void initModule(final Config config, final ServerContext serverContext) throws IOException {
        super.initModule(config, serverContext);

        final Set<Class<?>> endpointClasses = findWebSocketClasses();
        webSocketContainer = WsSci.onStartup(endpointClasses);
        serverContext.setAttribute("websocket.container", webSocketContainer);
    }


    /**
     * Finds the endpoint classes annotated with {@link ServerEndpoint}, extending {@link Endpoint}
     * or implementing {@link ServerApplicationConfig} .
     * 
     * @return all classes found or null
     */
    Set<Class<?>> findWebSocketClasses() {
        Set<Class<?>> classes = Reflections.findClassesWithAnnotation(ServerEndpoint.class);
        // filter out any Resteasy native resources
        classes = classes.stream().filter(c -> !c.getName().startsWith("org.apache.tomcat")).collect(Collectors.toSet());

        classes.addAll(Reflections.findClassesExtending(Endpoint.class));
        classes.addAll(Reflections.findClassesExtending(ServerApplicationConfig.class));

        return classes;
    }
}
