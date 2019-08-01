package com.airepublic.microprofile.plugin.http.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.microprofile.config.Config;

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpRequest;
import com.airepublic.http.common.pathmatcher.MappingResult;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.core.spi.Request;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.module.http.HttpChannelEncoder;
import com.airepublic.microprofile.plugin.http.websocket.server.WsSci;
import com.airepublic.microprofile.plugin.http.websocket.server.WsServerContainer;
import com.airepublic.reflections.Reflections;

public class WebSocketPlugin implements IServicePlugin {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private WsServerContainer webSocketContainer;
    @Inject
    private Config config;
    @Inject
    private IServerContext serverContext;
    private IServerModule module;


    @Override
    public void initPlugin(final IServerModule module) {
        this.module = module;

        try {
            final Set<Class<?>> endpointClasses = findWebSocketClasses();
            webSocketContainer = WsSci.onStartup(endpointClasses);
            serverContext.setAttribute("websocket.container", webSocketContainer);
        } catch (final IOException e) {
            throw new IllegalStateException("WebSocketContainer cound not be initialized!", e);
        }
    }


    @Override
    public String getName() {
        return getClass().getSimpleName();
    }


    @Override
    public Set<String> getSupportedProtocols() {
        return Set.of("HTTP");
    }


    @Override
    public int getPriority() {
        return 200;
    }


    @Override
    public IIOHandler determineIoHandler(final Request request) {
        final HttpRequest httpRequest = new HttpRequest(request.getAttributes().getString(HttpChannelEncoder.REQUEST_LINE), request.getAttributes().get(HttpChannelEncoder.HEADERS, Headers.class));
        httpRequest.setBody(request.getPayload());

        final String path = httpRequest.getPath();

        if (path == null) {
            return null;
        }

        final Class<? extends IIOHandler> handlerClass = findMapping(path);

        if (handlerClass != null) {
            try {
                return CDI.current().select(handlerClass).get();
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Could not instantiate handler: " + handlerClass, e);
            }
        }

        return null;
    }


    protected Class<? extends IIOHandler> findMapping(final String path) {
        if (webSocketContainer != null) {
            final MappingResult<ServerEndpointConfig> result = webSocketContainer.getMapping().findMapping(path);

            if (result != null) {
                return WebSocketIOHandler.class;
            }
        }

        return null;
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


    @Override
    public void close() throws Exception {
        webSocketContainer.destroy();
    }
}
