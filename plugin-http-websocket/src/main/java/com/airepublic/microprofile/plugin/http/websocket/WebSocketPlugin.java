package com.airepublic.microprofile.plugin.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.microprofile.config.Config;

import com.airepublic.microprofile.core.spi.DetermineStatus;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.core.spi.Pair;
import com.airepublic.microprofile.core.spi.Reflections;
import com.airepublic.microprofile.core.spi.SessionAttributes;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.plugin.http.websocket.server.WsSci;
import com.airepublic.microprofile.plugin.http.websocket.server.WsServerContainer;
import com.airepublic.microprofile.util.http.common.HttpBufferUtils;
import com.airepublic.microprofile.util.http.common.pathmatcher.MappingResult;

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
    public void onSessionCreate(final IServerSession session) {
    }


    @Override
    public Pair<DetermineStatus, IIOHandler> determineIoHandler(final ByteBuffer buffer, final SessionAttributes sessionAttributes) throws IOException {
        final String path = HttpBufferUtils.getUriPath(buffer);

        if (path == null) {
            return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
        }

        final Class<? extends IIOHandler> handlerClass = findMapping(path);

        if (handlerClass != null) {
            try {
                final IIOHandler handler = serverContext.getCdiContainer().select(handlerClass).get();

                return new Pair<>(DetermineStatus.TRUE, handler);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Could not instantiate handler: " + handlerClass, e);
                throw new IOException("Could not initialize handler: " + handlerClass, e);
            }
        }

        return new Pair<>(DetermineStatus.FALSE, null);
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
}
