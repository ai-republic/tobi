package com.airepublic.microprofile.module.http.jaxrs.resteasy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microprofile.core.Reflections;
import com.airepublic.microprofile.core.ServerContext;
import com.airepublic.microprofile.module.http.HttpModule;

public class RestEasyModule extends HttpModule {
    private final static Logger LOG = LoggerFactory.getLogger(RestEasyModule.class);
    public final static String CONTEXT_PATH = "rest.context.path";
    private final static String DEFAULT_JAXRS_CONTEXT_PATH = "/api";


    @Override
    public String getName() {
        return "JAX-RS Resteasy";
    }


    @Override
    public void initModule(final Config config, final ServerContext serverContext) throws IOException {
        super.initModule(config, serverContext);

        LOG.info("Configuring JAX-RS server...");
        final String defaultContextPath = config.getOptionalValue(CONTEXT_PATH, String.class).orElse(DEFAULT_JAXRS_CONTEXT_PATH);

        final RestEasyHttpContextBuilder contextBuilder = new RestEasyHttpContextBuilder();

        LOG.info("Searching for JAX-RS applications...");

        // check if there is an Application class with an @ApplicationPath annotation
        final Class<?> app = findApplicationClass();

        String contextPath = null;

        if (app != null) {
            LOG.info("Found JAX-RS application: " + app.getName());
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
            LOG.info("Determined context-path for JAX-RS application: " + app.getName() + " -> " + contextPath);

            Object appImpl = null;
            try {
                appImpl = serverContext.getCdiContainer().select(app).get();
            } catch (final Exception e) {
                LOG.error("Failed to instantiate Application class!", e);
            }

            if (appImpl != null) {
                try {
                    if (app.getMethod("getClasses") != null) {
                        for (final Class<?> resource : (Class[]) app.getMethod("getClasses").invoke(appImpl)) {
                            try {
                                addResource(contextPath, resource);
                            } catch (final Exception e) {
                                LOG.error("Failed to add JAX-RS resource: " + resource.getName());
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
                                LOG.error("Failed to add JAX-RS resource: " + resource.getClass().getName());
                            }
                        }
                    }
                } catch (final Exception e) {
                    // ignore
                }
            }
        } else {
            LOG.info("Found JAX-RS application: \n\t[]");
            LOG.info("Searching for JAX-RS resources...");
            contextPath = defaultContextPath;
            // find all resources with a @Path annotation
            final Set<Class<?>> resources = findResourceClasses();
            LOG.info("Found JAX-RS Resources:\n\t" + resources);

            if (resources != null) {
                for (final Class<?> resource : resources) {
                    try {
                        addResource(contextPath, resource);
                        contextBuilder.getDeployment().getScannedResourceClasses().add(resource.getName());
                    } catch (final Exception e) {
                        LOG.error("Failed to add JAX-RS resource: " + resource.getName());
                    }
                }
            }

            // use the configured or default context path
            contextBuilder.setPath(contextPath);
            LOG.info("Determined context-path for JAX-RS resources: " + contextPath);
        }

        contextBuilder.bind();
        serverContext.setAttribute("JAX-RS", contextBuilder);
        LOG.info("Finished configuring JAX-RS server!");
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
            LOG.info("Adding JAX-RS mapping for: " + resource.getName() + " -> " + rootPath);

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
                LOG.info("Adding JAX-RS mapping for: " + resource.getName() + ":" + method.getName() + " -> " + rootPath + subPath);
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
