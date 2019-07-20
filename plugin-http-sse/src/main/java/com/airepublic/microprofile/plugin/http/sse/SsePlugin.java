package com.airepublic.microprofile.plugin.http.sse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.sse.SseEventSource;

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
import com.airepublic.microprofile.util.http.common.BufferUtil;
import com.airepublic.microprofile.util.http.common.HttpBufferUtils;
import com.airepublic.microprofile.util.http.common.pathmatcher.PathMapping;

@Named
public class SsePlugin implements IServicePlugin {
    public static final String SSE_METHOD_MAPPING = "sse.method.mapping";
    public static final String SSE_SESSION_EVENTSOURCE = "sse.session.eventsource";
    public static final String SSE_SESSION_EVENTSOURCE_RECONNECTION_DELAY = "sse.session.eventsource.reconnection.delay";
    public static final String SSE_SESSION_EVENTSOURCE_ENDPOINT = "sse.session.eventsource.endpoint";
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    public final static String CONTEXT_PATH = "jax-rs.context.path";
    @Inject
    private Config config;
    @Inject
    private IServerContext serverContext;
    private final Map<String, Class<? extends IIOHandler>> mappings = new ConcurrentHashMap<>();
    private IServerModule module;
    private final PathMapping<Method> sseMethodMapping = new PathMapping<>();


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
    public Pair<DetermineStatus, IIOHandler> determineIoHandler(final ByteBuffer buffer, final SessionAttributes sessionAttributes) throws IOException {

        Class<? extends IIOHandler> handlerClass = null;

        try {
            // check for SSE outbound request
            buffer.mark();

            try {
                final String path = HttpBufferUtils.getUriPath(buffer);

                if (path == null) {
                    return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
                }

                handlerClass = findMapping(path);
            } finally {
                buffer.reset();
            }
        } catch (final Exception e) {
            // check for SSE inbound event
            final String line = BufferUtil.readLine(buffer, Charset.forName("UTF-8"));

            if (!line.contains(" HTTP/")) {
                handlerClass = SseInboundIOHandler.class;
            }
        }

        if (handlerClass != null) {
            try {
                final IIOHandler handler = CDI.current().select(handlerClass).get();

                return new Pair<>(DetermineStatus.TRUE, handler);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Could not instantiate handler: " + handlerClass, e);
                throw new IOException("Could not initialize handler: " + handlerClass, e);
            }
        }

        return new Pair<>(DetermineStatus.FALSE, null);
    }


    @Override
    public void onSessionCreate(final IServerSession session) {
        session.setAttribute(SSE_METHOD_MAPPING, sseMethodMapping);
    }


    public void addMapping(final String path, final Class<? extends IIOHandler> ioHandlerClass) {
        mappings.put(path, ioHandlerClass);
    }


    protected Class<? extends IIOHandler> findMapping(final String path) {
        return mappings.get(path);
    }


    @Override
    public void initPlugin(final IServerModule module) {
        this.module = module;

        logger.info("Searching for SSE applications...");

        // check if there is an Application class with an @ApplicationPath annotation
        final Class<?> app = findApplicationClass();

        String contextPath = null;

        if (app != null) {
            logger.info("Found SSE application: " + app.getName());
            // if an application was found use the value of the @ApplicationPath annotation as
            // context-path

            try {
                contextPath = Reflections.getClassAnnotationParameterValue(app, ApplicationPath.class, "value");
            } catch (final Exception e) {
                // otherwise use the configured or default context-path
            }

            if (contextPath == null || contextPath.isBlank()) {
                contextPath = "/";
            }

            logger.info("Determined context-path for SSE plugin: " + app.getName() + " -> " + contextPath);

        } else {
            contextPath = "/";
            logger.info("Found SSE application: \n\t[]");
            logger.info("Determined context-path for SSE resources: " + contextPath);
        }

        logger.info("Searching for SSE resources...");
        // find all resources with a @Path annotation
        final Set<Class<?>> resources = findResourceClasses();
        logger.info("Found SSE Resources:");

        if (resources != null) {
            for (final Class<?> resource : resources) {
                logger.info("\t" + resource.getName());

                try {
                    addResource(contextPath, resource);
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, "Failed to add SSE resource: " + resource.getName());
                }
            }
        }

        // use the configured or default context path
        logger.info("Finished configuring SSE server!");
    }


    /**
     * Adds mapping for JAX-RS resource by scanning the specified resource for {@link Path}
     * annotations.
     * 
     * @param contextPath the context path of the application or default path
     * @param resource the resource class to scan
     * @throws Exception
     */
    void addResource(final String contextPath, final Class<?> resource) throws Exception {
        try {
            if (!isSseResource(resource)) {
                return;
            }

            String rootPath = "";

            // check if Path annotation is present on class
            if (resource.isAnnotationPresent(Path.class)) {
                rootPath = Reflections.getClassAnnotationParameterValue(resource, Path.class, "value");

                if (rootPath != null && !rootPath.startsWith("/")) {
                    rootPath = "/" + rootPath;
                }
            }

            if (contextPath != null && !contextPath.isBlank()) {
                if (!contextPath.startsWith("/")) {
                    rootPath = "/" + contextPath + rootPath;
                } else {
                    rootPath = contextPath + rootPath;
                }
            }

            rootPath = rootPath.replace("//", "/");

            // check for Consumes annotated methods
            for (final Method method : Reflections.getAnnotatedMethods(resource, Produces.class, Consumes.class)) {
                String subPath = "";
                boolean sseMethodFound = false;
                boolean outbound = false;

                if (method.isAnnotationPresent(Produces.class)) {
                    final Produces annotation = method.getAnnotation(Produces.class);

                    sseMethodFound = Stream.of(annotation.value()).anyMatch(s -> s.contains("text/event-stream"));
                    outbound = true;
                } else if (method.isAnnotationPresent(Consumes.class)) {
                    final Consumes annotation = method.getAnnotation(Consumes.class);

                    sseMethodFound = Stream.of(annotation.value()).anyMatch(s -> s.contains("text/event-stream"));
                    outbound = false;
                }

                if (sseMethodFound) {
                    try {
                        subPath = Reflections.getMethodAnnotationParameterValue(method, Path.class, "value");

                        if (subPath != null && !subPath.isBlank() && !subPath.startsWith("/")) {
                            subPath = "/" + subPath;
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

                    if (outbound) {
                        addMapping(rootPath + subPath, SseOutboundIOHandler.class);
                        sseMethodMapping.add(rootPath + subPath, method);

                        logger.info("\t\tAdded SSE mapping for outbounded SSE events: " + resource.getName() + ":" + method.getName() + " -> " + rootPath + subPath);
                    } else {
                        try {
                            if (method.isAnnotationPresent(SseUri.class)) {
                                final URI uri = new URI(method.getAnnotation(SseUri.class).value());
                                final SseEventSourceImpl eventSource = (SseEventSourceImpl) SseEventSource.target(new SseWebTarget(uri)).build();
                                final Object obj = CDI.current().select(method.getDeclaringClass()).get();

                                eventSource.register(event -> {
                                    try {
                                        method.invoke(obj, event);
                                    } catch (final Exception e) {
                                    }
                                }, t -> logger.log(Level.SEVERE, "Error processing SSE inbound event!", t));

                                eventSource.setModule(module);
                                eventSource.setMethod(method);

                                addMapping(rootPath + subPath, SseInboundIOHandler.class);

                                eventSource.open();

                                logger.info("\t\tAdded SSE mapping for inbounded SSE events: " + resource.getName() + ":" + method.getName() + " -> " + rootPath + subPath);
                            } else {
                                logger.warning("SSE inbound event method is lacking the SseUri annotation to define the source of the events - it will not receive any events!");
                            }
                        } catch (final Exception e) {
                            logger.log(Level.SEVERE, "Error adding SSE resource!", e);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            // otherwise use the configured or default context-path
        }
    }


    /**
     * Finds the JAX-RS classes annotated with @ApplicationPath.
     * 
     * @return the first class found or null
     */
    Class<?> findApplicationClass() {
        final Set<Class<?>> classes = Reflections.findClassesWithAnnotation(ApplicationPath.class);
        return classes != null && classes.size() > 0 ? classes.iterator().next() : null;
    }


    /**
     * Finds the JAX-RS classes annotated with @Path.
     * 
     * @return all classes found or null
     */
    Set<Class<?>> findResourceClasses() {
        Set<Class<?>> classes = Reflections.findClassesWithAnnotation(Path.class);
        // filter out only SSE resources
        classes = classes.stream().filter(this::isSseResource).collect(Collectors.toSet());

        return classes;
    }


    boolean isSseResource(final Class<?> resource) {
        boolean sseMethodFound = true;

        // check for SSE Produces annotated methods (mimetype text/event-stream)
        for (final Method method : Reflections.getAnnotatedMethods(resource, Produces.class)) {
            final Produces annotation = method.getAnnotation(Produces.class);
            sseMethodFound = Stream.of(annotation.value()).anyMatch(s -> s.equals("text/event-stream"));

            if (sseMethodFound) {
                return true;
            }
        }

        // check for SSE Consumes annotated methods (mimetype text/event-stream)
        for (final Method method : Reflections.getAnnotatedMethods(resource, Consumes.class)) {
            final Consumes annotation = method.getAnnotation(Consumes.class);
            sseMethodFound = Stream.of(annotation.value()).anyMatch(s -> s.equals("text/event-stream"));

            if (sseMethodFound) {
                return true;
            }
        }

        return false;

    }
}
