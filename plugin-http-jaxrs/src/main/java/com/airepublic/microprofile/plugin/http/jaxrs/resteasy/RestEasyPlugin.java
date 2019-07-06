package com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.Config;

import com.airepublic.microprofile.core.spi.DetermineStatus;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.core.spi.Pair;
import com.airepublic.microprofile.core.spi.Reflections;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.util.http.common.HttpBufferUtils;

public class RestEasyPlugin implements IServicePlugin {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    public final static String CONTEXT_PATH = "jax-rs.context.path";
    @Inject
    private Config config;
    @Inject
    private IServerContext serverContext;
    private final Map<String, Class<? extends IIOHandler>> mappings = new ConcurrentHashMap<>();


    @Override
    public String getName() {
        return getClass().getSimpleName();
    }


    @Override
    public Set<String> getSupportedProtocols() {
        return Set.of("HTTP");
    }


    @Override
    public Pair<DetermineStatus, IIOHandler> determineIoHandler(final ByteBuffer buffer, final IServerSession session) throws IOException {
        final String path = HttpBufferUtils.getUriPath(buffer);

        if (path == null) {
            return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
        }

        final Class<? extends IIOHandler> handlerClass = findMapping(path);

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
    }


    public void addMapping(final String path, final Class<? extends IIOHandler> ioHandlerClass) {
        mappings.put(path, ioHandlerClass);
    }


    protected Class<? extends IIOHandler> findMapping(final String path) {
        return mappings.get(path);
    }


    @Override
    public void initPlugin(final IServerModule module) {
        final String defaultContextPath = config.getValue(CONTEXT_PATH, String.class);

        if (defaultContextPath == null) {
            throw new IllegalArgumentException("Configuration did not specify the '" + CONTEXT_PATH + "'!");
        }

        final RestEasyHttpContextBuilder contextBuilder = new RestEasyHttpContextBuilder();

        logger.info("Searching for JAX-RS applications...");

        // check if there is an Application class with an @ApplicationPath annotation
        final Class<?> app = findApplicationClass();

        String contextPath = null;

        if (app != null) {
            logger.info("Found JAX-RS application: " + app.getName());
            // if an application was found use the value of the @ApplicationPath annotation as
            // context-path
            contextBuilder.getDeployment().setApplicationClass(app.getName());

            try {
                contextPath = Reflections.getClassAnnotationParameterValue(app, ApplicationPath.class, "value");
            } catch (final Exception e) {
                // otherwise use the configured or default context-path
            }

            if (contextPath == null || contextPath.isBlank()) {
                contextPath = defaultContextPath;
            }

            contextBuilder.setPath(contextPath);
            logger.info("Determined context-path for JAX-RS application: " + app.getName() + " -> " + contextPath);

            Object appImpl = null;
            try {
                appImpl = CDI.current().select(app).get();
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Failed to instantiate JAX-RS Application class!", e);
            }

            if (appImpl != null) {
                try {
                    if (app.getMethod("getClasses") != null) {
                        for (final Class<?> resource : (Class[]) app.getMethod("getClasses").invoke(appImpl)) {
                            try {
                                addResource(contextPath, resource);
                            } catch (final Exception e) {
                                logger.log(Level.SEVERE, "Failed to add JAX-RS resource: " + resource.getName());
                            }
                        }
                    }
                } catch (final Exception e) {
                    // ignore
                }
                try {
                    if (app.getMethod("getSingletons") != null) {
                        for (final Object resource : (Object[]) app.getMethod("getSingletons").invoke(appImpl)) {
                            try {
                                addResource(contextPath, resource.getClass());
                            } catch (final Exception e) {
                                logger.log(Level.SEVERE, "Failed to add JAX-RS resource: " + resource.getClass().getName());
                            }
                        }
                    }
                } catch (final Exception e) {
                    // ignore
                }
            }
        } else {
            logger.info("Found JAX-RS application: \n\t[]");
            logger.info("Searching for JAX-RS resources...");
            contextPath = defaultContextPath;
            // find all resources with a @Path annotation
            final Set<Class<?>> resources = findResourceClasses();
            logger.info("Found JAX-RS Resources:\n\t" + resources);

            if (resources != null) {
                for (final Class<?> resource : resources) {
                    try {
                        addResource(contextPath, resource);
                        contextBuilder.getDeployment().getScannedResourceClasses().add(resource.getName());
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, "Failed to add JAX-RS resource: " + resource.getName());
                    }
                }
            }

            // use the configured or default context path
            contextBuilder.setPath(contextPath);
            logger.info("Determined context-path for JAX-RS resources: " + contextPath);
        }

        contextBuilder.bind();
        serverContext.setAttribute("JAX-RS", contextBuilder);
        logger.info("Finished configuring JAX-RS server!");
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
            if (isSseResource(resource)) {
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

            addMapping(rootPath, RestEasyIOHandler.class);
            logger.info("Adding JAX-RS mapping for: " + resource.getName() + " -> " + rootPath);

            // check for Path annotated methods
            for (final Method method : Reflections.getAnnotatedMethods(resource, Path.class)) {
                String subPath = "";

                try {
                    subPath = Reflections.getMethodAnnotationParameterValue(method, Path.class, "value");

                    if (subPath != null && !subPath.isBlank() && !subPath.startsWith("/")) {
                        subPath = "/" + subPath;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                addMapping(rootPath + subPath, RestEasyIOHandler.class);
                logger.info("Adding JAX-RS mapping for: " + resource.getName() + ":" + method.getName() + " -> " + rootPath + subPath);
            }
        } catch (final Exception e) {
            // otherwise use the configured or default context-path
        }
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
        // filter out any Resteasy native resources
        classes = classes.stream().filter(c -> !c.getName().startsWith("org.jboss.resteasy")).collect(Collectors.toSet());
        return classes;
    }

}
