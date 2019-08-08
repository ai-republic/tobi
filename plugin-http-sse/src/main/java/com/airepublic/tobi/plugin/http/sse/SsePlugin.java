package com.airepublic.tobi.plugin.http.sse;

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

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpRequest;
import com.airepublic.http.sse.api.ISseRegistry;
import com.airepublic.http.sse.api.ISseService;
import com.airepublic.http.sse.api.ProducerEntry;
import com.airepublic.http.sse.api.SseConsumer;
import com.airepublic.http.sse.api.SseProducer;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.reflections.Reflections;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IServerModule;
import com.airepublic.tobi.core.spi.IServicePlugin;
import com.airepublic.tobi.core.spi.Request;
import com.airepublic.tobi.module.http.HttpChannelEncoder;

/**
 * The {@link IServicePlugin} implementation for SSE.
 * 
 * @author Torsten Oltmanns
 *
 */
@Named
public class SsePlugin implements IServicePlugin {
    public static final String SSE_SERVICE_METHOD = "sse.service.method";
    public static final String SSE_SERVICE_OBJECT = "sse.service.object";
    public static final String SSE_SESSION_RECONNECTION_DELAY = "sse.session.eventsource.reconnection.delay";
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    public final static String CONTEXT_PATH = "jax-rs.context.path";
    @Inject
    private ISseRegistry sseRegistry;
    @Inject
    private ISseService sseService;


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
    public IIOHandler determineIoHandler(final Request request) {

        final Class<? extends IIOHandler> handlerClass = null;
        final HttpRequest httpRequest = new HttpRequest(request.getAttributes().getString(HttpChannelEncoder.REQUEST_LINE), request.getAttributes().get(HttpChannelEncoder.HEADERS, Headers.class));
        httpRequest.setBody(request.getPayload());

        final String path = httpRequest.getPath();
        final ProducerEntry producerEntry = sseRegistry.getSseProducer(path);

        if (producerEntry != null) {
            try {
                final SseOutboundIOHandler handler = CDI.current().select(SseOutboundIOHandler.class).get();
                handler.setServiceObject(producerEntry.getObject());
                handler.setServiceMethod(producerEntry.getMethod());

                return handler;
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Could not instantiate handler: " + handlerClass, e);
            }
        }

        return null;
    }


    @Override
    public void initPlugin(final IServerModule module) {

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
     * Adds SSE resources by scanning the specified resource for {@link SseProducer} or
     * {@link SseConsumer} annotations.
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
                    sseRegistry.registerSseProducer(annotation.path(), resource, method);

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

                        sseRegistry.registerSseConsumer(uri, sseConsumer);

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


    /**
     * Opens the {@link SocketChannel} to a client.
     * 
     * @param uri
     * @return
     */
    static SocketChannel openChannel(final URI uri) {
        // initialize port with default value
        int port = uri.getScheme().equals("https") ? 443 : 80;
        SocketChannel channel;

        // check for a custom port
        if (uri.getPort() > 0) {
            port = uri.getPort();
        }

        try {
            // connect to the client
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
