package com.airepublic.microprofile.plugin.http.sse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;

import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IRequest;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.core.spi.Reflections;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.plugin.http.sse.api.SseConsumer;
import com.airepublic.microprofile.plugin.http.sse.api.SseProducer;
import com.airepublic.microprofile.plugin.http.sse.api.SseRegistry;
import com.airepublic.microprofile.plugin.http.sse.api.SseService;
import com.airepublic.microprofile.util.http.common.HttpRequest;

@Named
public class SsePlugin implements IServicePlugin {
    public static final String SSE_SERVICE_METHOD = "sse.service.method";
    public static final String SSE_SERVICE_OBJECT = "sse.service.object";
    public static final String SSE_SESSION_RECONNECTION_DELAY = "sse.session.eventsource.reconnection.delay";
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    public final static String CONTEXT_PATH = "jax-rs.context.path";
    private IServerModule module;
    @Inject
    private SseRegistry sseRegistry;
    @Inject
    private SseService sseService;


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
        return 100;
    }


    @Override
    public IIOHandler determineIoHandler(final IRequest request) {

        final Class<? extends IIOHandler> handlerClass = null;

        final String path = ((HttpRequest) request).getPath();
        final Method serviceMethod = sseRegistry.getSseProducer(path);

        if (serviceMethod != null) {
            try {
                final SseOutboundIOHandler handler = CDI.current().select(SseOutboundIOHandler.class).get();
                handler.setServiceMethod(serviceMethod);
                handler.setServiceObject(sseRegistry.getObject(path));

                return handler;
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Could not instantiate handler: " + handlerClass, e);
            }
        }

        return null;
    }


    @Override
    public void initPlugin(final IServerModule module) {
        this.module = module;

        logger.info("Searching for SSE resources...");

        final Set<Class<?>> resources = findSseResources();

        if (resources != null && !resources.isEmpty()) {
            logger.info("Found SSE Resources:");

            for (final Class<?> resource : resources) {
                logger.info("\t" + resource.getName());

                try {
                    addResource(resource);
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, "Failed to add SSE resource: " + resource.getName());
                }
            }
        } else {
            logger.info("No SSE Resources found!");
        }

        // use the configured or default context path
        logger.info("Finished configuring SSE server!");
    }


    /**
     * Adds mapping for JAX-RS resource by scanning the specified resource for {@link Path}
     * annotations.
     * 
     * @param resource the resource class to scan
     * @throws Exception
     */
    void addResource(final Class<?> resource) throws Exception {
        try {

            // check for SseProducer or SseConsumer annotated methods
            for (final Method method : Reflections.getAnnotatedMethods(resource, SseProducer.class, SseConsumer.class)) {

                if (method.isAnnotationPresent(SseProducer.class)) {
                    final SseProducer annotation = method.getAnnotation(SseProducer.class);
                    sseRegistry.addSseProducer(annotation.path(), resource, method);

                    logger.info("\t\tAdded SSE mapping for outbounded SSE events: " + resource.getName() + ":" + method.getName() + " -> " + annotation.path());
                } else if (method.isAnnotationPresent(SseConsumer.class)) {
                    try {
                        final SseConsumer annotation = method.getAnnotation(SseConsumer.class);
                        final URI uri = new URI(annotation.value());
                        final Object object = CDI.current().select(resource).get();

                        final Future<Void> sseConsumer = sseService.receive(uri, event -> {
                            try {
                                method.invoke(object, event);
                            } catch (final Exception e) {
                                logger.log(Level.SEVERE, "Error invoking SSE event method:", e);
                            }
                        });

                        sseRegistry.addSseConsumer(uri, sseConsumer);

                        logger.info("\t\tAdded SSE mapping for inbounded SSE events: " + resource.getName() + ":" + method.getName() + " -> " + uri);
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, "Error adding SSE resource!", e);
                    }
                } else {
                    logger.warning("SSE inbound event method is lacking the SseConsumer annotation to define the source of the events - it will not receive any events!");
                }
            }

        } catch (final Exception e) {
            // otherwise use the configured or default context-path
        }
    }


    static SocketChannel openChannel(final URI uri) {
        int port = uri.getScheme().equals("https") ? 443 : 80;
        SocketChannel channel;

        if (uri.getPort() > 0) {
            port = uri.getPort();
        }

        try {
            final SocketAddress remote = new InetSocketAddress(uri.getHost(), port);
            channel = SocketChannel.open();
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            channel.configureBlocking(false);
            channel.connect(remote);

            while (!channel.finishConnect()) {
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create socket channel!", e);
        }

        return channel;
    }


    /**
     * Finds the SSE classes annotated with {@link SseProducer} or {@link SseConsumer}.
     * 
     * @return all classes found or null
     */
    Set<Class<?>> findSseResources() {
        return Reflections.findClassesWithMethodAnnotations(SseProducer.class, SseConsumer.class);
    }


    @Override
    public void close() throws Exception {
        sseRegistry.close();
    }
}
