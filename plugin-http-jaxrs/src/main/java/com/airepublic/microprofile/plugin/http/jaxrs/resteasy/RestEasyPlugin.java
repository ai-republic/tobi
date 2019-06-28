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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.Config;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.DetermineStatus;
import com.airepublic.microprofile.core.IServicePlugin;
import com.airepublic.microprofile.core.Pair;
import com.airepublic.microprofile.core.Reflections;
import com.airepublic.microprofile.core.ServerContext;
import com.airepublic.microprofile.core.ServerSession;
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
    private ServerContext serverContext;
    private final Map<String, Class<? extends AbstractIOHandler>> mappings = new ConcurrentHashMap<>();


    @Override
    public String getName() {
        return "JAX-RS Resteasy plugin";
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
                final RestEasyIOHandler handler = serverContext.getCdiContainer().select(RestEasyIOHandler.class).get();
                handler.init(session);
                return new Pair<>(DetermineStatus.TRUE, handler);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Could not instantiate handler: " + RestEasyIOHandler.class, e);
                throw new IOException("Could not initialize handler: " + RestEasyIOHandler.class, e);
            }
        }

        return new Pair<>(DetermineStatus.FALSE, null);
    }


    public void addMapping(final String path, final Class<? extends AbstractIOHandler> ioHandlerClass) {
        mappings.put(path, ioHandlerClass);
    }


    protected Class<? extends AbstractIOHandler> findMapping(final String path) {
        return mappings.get(path);
    }


    @PostConstruct
    public void initPlugin() {
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
                appImpl = serverContext.getCdiContainer().select(app).get();
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Failed to instantiate Application class!", e);
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
