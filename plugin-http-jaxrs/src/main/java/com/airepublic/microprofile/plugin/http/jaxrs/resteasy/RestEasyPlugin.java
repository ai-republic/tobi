package com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
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

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.DefaultResourceClass;
import org.jboss.resteasy.spi.metadata.DefaultResourceMethod;
import org.jboss.resteasy.spi.metadata.ResourceClass;

import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IRequest;
import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.core.spi.Reflections;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.util.http.common.HttpRequest;
import com.airepublic.microprofile.util.http.common.HttpResponse;
import com.airepublic.microprofile.util.http.common.HttpStatus;

public class RestEasyPlugin implements IServicePlugin {
    public static final String CONTEXT_BUILDER = "http.jaxrs.resteasy.ContextBuilder";
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private IServerContext serverContext;
    private String contextPath;
    private final Set<String> mappings = new HashSet<>();
    private RestEasyHttpContextBuilder contextBuilder;


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
        return 300;
    }


    @Override
    public IIOHandler determineIoHandler(final IRequest request) {
        try {
            final HttpResponse response = new HttpResponse(HttpStatus.OK);
            final RestEasyHttpResponseWrapper restEasyHttpResponse = new RestEasyHttpResponseWrapper(response, null);
            final RestEasyHttpRequestWrapper restEasyHttpRequest = new RestEasyHttpRequestWrapper((HttpRequest) request, restEasyHttpResponse, (SynchronousDispatcher) contextBuilder.getDeployment().getDispatcher(), contextPath);
            final ResourceInvoker invoker = ((ResourceMethodRegistry) contextBuilder.getDeployment().getRegistry()).getResourceInvoker(restEasyHttpRequest);

            if (invoker != null) {
                restEasyHttpRequest.setAttribute(invoker.getClass().getName(), invoker);

                final ResourceClass resourceClass = new DefaultResourceClass(invoker.getMethod().getDeclaringClass(), restEasyHttpRequest.getUri().getPath());
                final POJOResourceFactory rf = new POJOResourceFactory(resourceClass);
                final ResourceMethodInvoker methodInvoker = new ResourceMethodInvoker(new DefaultResourceMethod(resourceClass, invoker.getMethod(), invoker.getMethod()), contextBuilder.getDeployment().getInjectorFactory(), rf, contextBuilder.getDeployment().getProviderFactory());
                restEasyHttpRequest.setAttribute(ResourceMethodInvoker.class.getName(), methodInvoker);

                try {
                    final RestEasyIOHandler handler = CDI.current().select(RestEasyIOHandler.class).get();
                    return handler;
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, "Could not instantiate handler: " + RestEasyIOHandler.class, e);
                }
            }
        } catch (final Exception e) {
        }

        return null;
    }


    public void addMapping(final String path) {
        mappings.add(path);
    }


    @Override
    public void initPlugin(final IServerModule module) {

        contextBuilder = new RestEasyHttpContextBuilder();
        contextBuilder.getDeployment().setInjectorFactory(new CdiInjectorFactory(CDI.current().getBeanManager()));
        contextBuilder.getDeployment().setRegistry(new ResourceMethodRegistry(ResteasyProviderFactory.getInstance()));

        logger.info("Searching for JAX-RS applications...");

        // check if there is an Application class with an @ApplicationPath annotation
        final Class<?> app = findApplicationClass();

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
                contextPath = "/";
            }

            contextBuilder.setPath(contextPath);
            logger.info("Determined context-path for JAX-RS application: " + app.getName() + " -> " + contextPath);

        } else {
            logger.info("Found JAX-RS application: \n\t[]");
        }

        logger.info("Searching for JAX-RS resources...");
        contextPath = "/";
        // find all resources with a @Path annotation
        final Set<Class<?>> resources = findResourceClasses();

        if (resources != null) {
            logger.info("Found JAX-RS Resources:");
            for (final Class<?> resource : resources) {
                logger.info("\t" + resource.getName());

                try {
                    addResource(contextPath, resource);
                    contextBuilder.getDeployment().getScannedResourceClasses().add(resource.getName());
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, "Failed to add JAX-RS resource: " + resource.getName(), e);
                }
            }
        } else {
            logger.info("Found JAX-RS Resources:\n\t[]");
        }

        // use the configured or default context path
        contextBuilder.setPath(contextPath);
        logger.info("Determined context-path for JAX-RS resources: " + contextPath);

        contextBuilder.bind();
        serverContext.setAttribute(CONTEXT_BUILDER, contextBuilder);
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

            rootPath = rootPath.replace("//", "/");

            addMapping(rootPath);
            logger.info("\t\tAdding JAX-RS mapping for: " + resource.getName() + " -> " + rootPath);

            // check for Path annotated methods
            for (final Method method : Reflections.getAnnotatedMethods(resource, Path.class)) {
                // if (!isSseResource(resource, method)) {
                String subPath = "";

                try {
                    subPath = Reflections.getMethodAnnotationParameterValue(method, Path.class, "value");

                    if (subPath != null && !subPath.isBlank() && !subPath.startsWith("/")) {
                        subPath = "/" + subPath;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                addMapping(rootPath + subPath);
                logger.info("\t\tAdding JAX-RS mapping for: " + resource.getName() + ":" + method.getName() + " -> " + rootPath + subPath);
                // }
            }
        } catch (final Exception e) {
            // otherwise use the configured or default context-path
        }
    }


    boolean isSseResource(final Class<?> resource, final Method method) {
        boolean sseMethodFound = false;

        // check for SSE Produces annotated methods (mimetype text/event-stream)
        final Produces producesAnnotation = method.getAnnotation(Produces.class);

        if (producesAnnotation != null) {
            sseMethodFound = Stream.of(producesAnnotation.value()).anyMatch(v -> v.equals("text/event-stream"));
        }

        // check for SSE Consumes annotated methods (mimetype text/event-stream)
        final Consumes consumesAnnotation = method.getAnnotation(Consumes.class);

        if (!sseMethodFound && consumesAnnotation != null) {
            sseMethodFound = Stream.of(consumesAnnotation.value()).anyMatch(v -> v.equals("text/event-stream"));
        }

        return sseMethodFound;
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


    @Override
    public void close() throws Exception {
        contextBuilder.getDeployment().stop();
        contextBuilder.cleanup();
    }
}
