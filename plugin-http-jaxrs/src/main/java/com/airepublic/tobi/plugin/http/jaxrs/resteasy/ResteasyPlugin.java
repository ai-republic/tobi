package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

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
import javax.ws.rs.core.Application;

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

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpRequest;
import com.airepublic.http.common.HttpResponse;
import com.airepublic.http.common.HttpStatus;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.reflections.Reflections;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IServerContext;
import com.airepublic.tobi.core.spi.IServerModule;
import com.airepublic.tobi.core.spi.IServicePlugin;
import com.airepublic.tobi.core.spi.Request;
import com.airepublic.tobi.module.http.HttpChannelEncoder;

/**
 * The {@link IServicePlugin} implementation for JAX-RS using resteasy.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ResteasyPlugin implements IServicePlugin {
    public static final String CONTEXT_BUILDER = "http.jaxrs.resteasy.ContextBuilder";
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private IServerContext serverContext;
    private String contextPath;
    private ResteasyHttpContextBuilder contextBuilder;


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
    public IIOHandler determineIoHandler(final Request request) {
        try {
            final HttpResponse response = new HttpResponse(HttpStatus.OK);
            final ResteasyHttpResponseWrapper restEasyHttpResponse = new ResteasyHttpResponseWrapper(response, null);
            final HttpRequest httpRequest = new HttpRequest(request.getString(HttpChannelEncoder.REQUEST_LINE), request.getAttribute(HttpChannelEncoder.HEADERS, Headers.class));
            httpRequest.setBody(request.getPayload());
            final ResteasyHttpRequestWrapper restEasyHttpRequest = new ResteasyHttpRequestWrapper(httpRequest, restEasyHttpResponse, (SynchronousDispatcher) contextBuilder.getDeployment().getDispatcher(), contextPath);
            final ResourceInvoker invoker = ((ResourceMethodRegistry) contextBuilder.getDeployment().getRegistry()).getResourceInvoker(restEasyHttpRequest);

            if (invoker != null) {
                restEasyHttpRequest.setAttribute(invoker.getClass().getName(), invoker);

                final ResourceClass resourceClass = new DefaultResourceClass(invoker.getMethod().getDeclaringClass(), restEasyHttpRequest.getUri().getPath());
                final POJOResourceFactory rf = new POJOResourceFactory(resourceClass);
                final ResourceMethodInvoker methodInvoker = new ResourceMethodInvoker(new DefaultResourceMethod(resourceClass, invoker.getMethod(), invoker.getMethod()), contextBuilder.getDeployment().getInjectorFactory(), rf, contextBuilder.getDeployment().getProviderFactory());
                restEasyHttpRequest.setAttribute(ResourceMethodInvoker.class.getName(), methodInvoker);

                try {
                    final ResteasyIOHandler handler = CDI.current().select(ResteasyIOHandler.class).get();
                    return handler;
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, "Could not instantiate handler: " + ResteasyIOHandler.class, e);
                }
            }
        } catch (final Exception e) {
        }

        return null;
    }


    @Override
    public void initPlugin(final IServerModule module) {

        contextBuilder = new ResteasyHttpContextBuilder();
        contextBuilder.getDeployment().setInjectorFactory(new CdiInjectorFactory(CDI.current().getBeanManager()));
        contextBuilder.getDeployment().setRegistry(new ResourceMethodRegistry(ResteasyProviderFactory.getInstance()));
        contextBuilder.getDeployment().getProviderClasses().add(ObjectMapperContextResolver.class.getName());

        logger.info("Searching for JAX-RS applications...");
        final Set<Class<?>> resourceClasses = new HashSet<>();

        // check if there is an Application class with an @ApplicationPath annotation
        final Class<?> app = findApplicationClass();

        contextPath = "/";

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

            final String path = contextPath;

            // add application resources under the root application path
            try {
                final Application application = (Application) app.getConstructor().newInstance();
                contextBuilder.getDeployment().setApplicationClass(app.getName());

                application.getClasses().forEach(resource -> {
                    try {
                        // addResource(path, resource);
                        resourceClasses.add(resource);
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, "Failed to add JAX-RS resource: " + resource.getName(), e);
                    }
                });
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Failed to instantiate JAX-RS application: " + app.getName(), e);
            }

        } else {
            logger.info("Found JAX-RS application: \n\t[]");

            // use the configured or default context path
            contextBuilder.setPath("/");
        }

        // search for other non application-defined resources
        logger.info("Searching for JAX-RS resources...");
        // find all resources with a @Path annotation
        final Set<Class<?>> resources = findResourceClasses();

        if (resources != null) {
            logger.info("Found JAX-RS Resources:");
            resources.stream().filter(resource -> !resourceClasses.contains(resource)).forEach(resource -> {
                logger.info("\t" + resource.getName());

                try {
                    addResource("/", resource);
                    // contextBuilder.getDeployment().getScannedResourceClasses().add(resource.getName());
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, "Failed to add JAX-RS resource: " + resource.getName(), e);
                }
            });
        } else {
            logger.info("Found JAX-RS Resources:\n\t[]");
        }

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

                logger.info("\t\tAdding JAX-RS mapping for: " + resource.getName() + ":" + method.getName() + " -> " + rootPath + subPath);
                // }
            }
        } catch (final Exception e) {
            // otherwise use the configured or default context-path
        }
    }


    /**
     * Checks if the specified resource method represents a SSE producer or consumer.
     * 
     * @param resource the resource class
     * @param method the resource method
     * @return true if the method represents a SSE producer or consumer
     */
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
