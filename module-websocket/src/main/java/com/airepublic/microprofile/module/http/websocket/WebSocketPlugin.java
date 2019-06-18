package com.airepublic.microprofile.module.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.DetermineStatus;
import com.airepublic.microprofile.core.IServicePlugin;
import com.airepublic.microprofile.core.Pair;
import com.airepublic.microprofile.core.Reflections;
import com.airepublic.microprofile.core.ServerContext;
import com.airepublic.microprofile.core.ServerSession;
import com.airepublic.microprofile.core.pathmatcher.MappingResult;
import com.airepublic.microprofile.module.http.core.HttpBufferUtils;
import com.airepublic.microprofile.module.http.websocket.server.WsSci;
import com.airepublic.microprofile.module.http.websocket.server.WsServerContainer;

public class WebSocketPlugin implements IServicePlugin {
    private final static Logger LOG = LoggerFactory.getLogger(WebSocketIOHandler.class);
    private WsServerContainer webSocketContainer;
    @Inject
    private Config config;
    @Inject
    private ServerContext serverContext;


    @Override
    public String getName() {
        return "WebSocket plugin";
    }


    @Override
    public Set<String> getSupportedProtocols() {
        return Set.of("HTTP");
    }


    @Override
    public Pair<DetermineStatus, AbstractIOHandler> determineIoHandler(final ByteBuffer buffer, final ServerSession session) throws IOException {
        final String path = HttpBufferUtils.getUriPath(buffer);

        if (path == null) {
            return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
        }

        if (findMapping(path) != null) {
            try {
                final WebSocketIOHandler handler = serverContext.getCdiContainer().select(WebSocketIOHandler.class).get();
                handler.init(session);
                return new Pair<>(DetermineStatus.TRUE, handler);
            } catch (final Exception e) {
                LOG.error("Could not instantiate handler: " + WebSocketIOHandler.class, e);
                throw new IOException("Could not initialize handler: " + WebSocketIOHandler.class, e);
            }
        }

        return new Pair<>(DetermineStatus.FALSE, null);
    }


    protected Class<? extends AbstractIOHandler> findMapping(final String path) {
        if (webSocketContainer != null) {
            final MappingResult<ServerEndpointConfig> result = webSocketContainer.getMapping().findMapping(path);

            if (result != null) {
                return WebSocketIOHandler.class;
            }
        }

        return null;
    }


    @PostConstruct
    public void initPlugin() {

        try {
            final Set<Class<?>> endpointClasses = findWebSocketClasses();
            webSocketContainer = WsSci.onStartup(endpointClasses);
            serverContext.setAttribute("websocket.container", webSocketContainer);
        } catch (final IOException e) {
            throw new IllegalStateException("WebSocketContainer cound not be initialized!", e);
        }
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
