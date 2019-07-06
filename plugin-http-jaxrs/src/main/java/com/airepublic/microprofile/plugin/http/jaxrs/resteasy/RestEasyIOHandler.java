package com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

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
    protected SynchronousDispatcher dispatcher;
    protected ResteasyProviderFactory providerFactory;
    private RestEasyHttpContextBuilder contextBuilder;
    private String contextPath;
    private HttpResponse response;


    @PostConstruct
    public void init() {
        if (serverContext.hasAttribute("JAX-RS")) {
            contextBuilder = (RestEasyHttpContextBuilder) serverContext.getAttribute("JAX-RS");
        } else {
            throw new IllegalStateException(RestEasyHttpContextBuilder.class.getSimpleName() + " has not been set in the server-context under JAX-RS key!");
        }

        setDispatcher(contextBuilder.getDeployment().getDispatcher());
        setProviderFactory(contextBuilder.getDeployment().getProviderFactory());
        contextPath = contextBuilder.getPath();
    }


    public void setDispatcher(final Dispatcher dispatcher) {
        this.dispatcher = SynchronousDispatcher.class.cast(dispatcher);
    }


    public void setProviderFactory(final ResteasyProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }


    private String determineContentType(final ByteBuffer body) {
        // TODO determine content type
        return "text/plain";
    }


    @Override
    public HttpResponse getHttpResponse() {
        if (response == null) {
            try {
                // logger.info("***PATH: " + request.getRequestURL());
                // classloader/deployment aware RestasyProviderFactory. Used to have request
                // specific
                // ResteasyProviderFactory.getInstance()
                final ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
                if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
                    ThreadLocalResteasyProviderFactory.push(providerFactory);
                }

                response = new HttpResponse(HttpStatus.OK);

                try {
                    // final HttpCoreContext coreContext = HttpCoreContext.adapt(context);
                    // ResteasyProviderFactory.pushContext(HttpContext.class, context);
                    // ResteasyProviderFactory.pushContext(HttpCoreContext.class, coreContext);

                    final RestEasyHttpResponseWrapper restEasyHttpResponse = new RestEasyHttpResponseWrapper(response, this);
                    final RestEasyHttpRequestWrapper restEasyHttpRequest = new RestEasyHttpRequestWrapper(getHttpRequest(), restEasyHttpResponse, dispatcher, contextPath);

                    dispatcher.invoke(restEasyHttpRequest, restEasyHttpResponse);

                    // write headers and body to HttpResponse
                    restEasyHttpResponse.mergeToResponse();

                    final String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

                    if (contentType == null) {
                        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, determineContentType(response.getBody()));
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
