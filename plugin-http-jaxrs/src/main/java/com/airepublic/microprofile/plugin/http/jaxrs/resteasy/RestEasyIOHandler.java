package com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.sse.SseEventOutputImpl;
import org.jboss.resteasy.plugins.providers.sse.SseEventProvider;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.DefaultResourceClass;
import org.jboss.resteasy.spi.metadata.DefaultResourceMethod;
import org.jboss.resteasy.spi.metadata.ResourceClass;

import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.util.http.common.AbstractHttpIOHandler;
import com.airepublic.microprofile.util.http.common.HttpResponse;
import com.airepublic.microprofile.util.http.common.HttpStatus;

public class RestEasyIOHandler extends AbstractHttpIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private IServerContext serverContext;
    private RestEasyHttpContextBuilder contextBuilder;
    private String contextPath;
    private HttpResponse response;


    @PostConstruct
    public void init() {
        if (serverContext.hasAttribute(RestEasyPlugin.CONTEXT_BUILDER)) {
            contextBuilder = (RestEasyHttpContextBuilder) serverContext.getAttribute(RestEasyPlugin.CONTEXT_BUILDER);
        } else {
            throw new IllegalStateException(RestEasyHttpContextBuilder.class.getSimpleName() + " has not been set in the server-context under JAX-RS key!");
        }

        contextPath = contextBuilder.getPath();
    }


    @Override
    public void onSessionClose() {
    }


    private String determineContentType(final ByteBuffer body) {
        // TODO determine content type
        return "text/plain";
    }


    @Override
    public HttpResponse getHttpResponse() {
        if (response == null) {
            try {
                final ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();

                if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
                    ThreadLocalResteasyProviderFactory.push(contextBuilder.getDeployment().getProviderFactory());
                }

                response = new HttpResponse(HttpStatus.OK);

                try {
                    // create the Resteasy request and response wrappers
                    final RestEasyHttpResponseWrapper restEasyHttpResponse = new RestEasyHttpResponseWrapper(response, this);
                    final RestEasyHttpRequestWrapper restEasyHttpRequest = new RestEasyHttpRequestWrapper(getHttpRequest(), restEasyHttpResponse, (SynchronousDispatcher) contextBuilder.getDeployment().getDispatcher(), contextPath);

                    // add them to the context
                    ResteasyProviderFactory.getContextDataMap().put(HttpRequest.class, restEasyHttpRequest);
                    ResteasyProviderFactory.getContextDataMap().put(org.jboss.resteasy.spi.HttpResponse.class, restEasyHttpResponse);

                    // provide a SSE event sink
                    final SseEventSink sseEventSink = new SseEventOutputImpl(new SseEventProvider());
                    ResteasyProviderFactory.getContextDataMap().put(SseEventSink.class, sseEventSink);

                    final ResourceInvoker invoker = ((ResourceMethodRegistry) contextBuilder.getDeployment().getRegistry()).getResourceInvoker(restEasyHttpRequest);

                    if (invoker != null) {
                        restEasyHttpRequest.setAttribute(invoker.getClass().getName(), invoker);

                        final ResourceClass resourceClass = new DefaultResourceClass(invoker.getMethod().getDeclaringClass(), restEasyHttpRequest.getUri().getPath());
                        final POJOResourceFactory rf = new POJOResourceFactory(resourceClass);
                        final ResourceMethodInvoker methodInvoker = new ResourceMethodInvoker(new DefaultResourceMethod(resourceClass, invoker.getMethod(), invoker.getMethod()), contextBuilder.getDeployment().getInjectorFactory(), rf, contextBuilder.getDeployment().getProviderFactory());
                        restEasyHttpRequest.setAttribute(ResourceMethodInvoker.class.getName(), methodInvoker);
                    }

                    contextBuilder.getDeployment().getDispatcher().invoke(restEasyHttpRequest, restEasyHttpResponse);

                    // write headers and body to HttpResponse
                    restEasyHttpResponse.mergeToResponse();

                    final String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

                    if (contentType == null) {
                        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, determineContentType(response.getBody()));
                    }

                    // fix a bug in Resteasy which returns a 404 for SSE
                    if (contentType.equals(MediaType.SERVER_SENT_EVENTS) && response.getStatus() == HttpStatus.NOT_FOUND && response.getBody() != null) {
                        response.withStatus(HttpStatus.SUCCESS);
                    }

                } catch (final Exception ex) {
                    logger.log(Level.SEVERE, "Error submitting JAX-RS response!", ex);

                    try {
                        response = new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
                    } catch (final Exception e) {
                    }
                } finally {
                    ResteasyProviderFactory.clearContextData();
                }
            } finally {
                final ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
                if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
                    ThreadLocalResteasyProviderFactory.pop();
                }
            }
        }

        return response;
    }


    public void setHttpResponse(final HttpResponse response) {
        this.response = response;
    }
}
