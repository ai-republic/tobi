package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
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
import com.airepublic.tobi.core.spi.ChannelAction;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IServerContext;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Request;
import com.airepublic.tobi.module.http.HttpChannelEncoder;

/**
 * The {@link IIOHandler} implementation for JAX-RS using resteasy.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ResteasyIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private IServerContext serverContext;
    private ResteasyHttpContextBuilder contextBuilder;
    private String contextPath;
    private HttpResponse response;
    private Request request;
    @Inject
    private IServerSession session;


    /**
     * Initializes this handler.
     */
    @PostConstruct
    public void init() {
        if (serverContext.hasAttribute(ResteasyPlugin.CONTEXT_BUILDER)) {
            contextBuilder = (ResteasyHttpContextBuilder) serverContext.getAttribute(ResteasyPlugin.CONTEXT_BUILDER);
        } else {
            throw new IllegalStateException(ResteasyHttpContextBuilder.class.getSimpleName() + " has not been set in the server-context under JAX-RS key!");
        }

        contextPath = contextBuilder.getPath();
    }


    /**
     * The default implementation will just send a {@link ChannelAction#CLOSE_INPUT}.
     * 
     * @param request the {@link Request} read from the incoming stream
     * @return the {@link ChannelAction#CLOSE_INPUT}
     * @throws IOException if something goes wrong during processing
     */
    @Override
    public ChannelAction consume(final Request request) throws IOException {
        this.request = request;
        return ChannelAction.CLOSE_INPUT;
    }


    /**
     * The default implementation tries to generate a {@link HttpResponse} by calling
     * {@link AbstractHttpIOHandler#getHttpResponse()} and writing the header- and body
     * {@link ByteBuffer} to the write-buffer-queue.
     * 
     * @throws IOException if something goes wrong during producing the {@link HttpResponse}
     */
    @Override
    public void produce() throws IOException {
        final HttpResponse response = getHttpResponse();

        session.getChannelProcessor().addToWriteBuffer(response.getHeaderBuffer(), response.getBody());
    }


    @Override
    public void onSessionClose() {
    }


    /**
     * Determine the content-type of the content of the body.
     * 
     * @param body the body {@link ByteBuffer}
     * @return the content-type
     */
    private String determineContentType(final ByteBuffer body) {
        // TODO determine content type
        return "text/plain";
    }


    /**
     * The implementation should produce a {@link HttpResponse} corresponding to the
     * {@link HttpRequest}.
     * 
     * @return a {@link HttpResponse}
     */
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
                    final HttpRequest httpRequest = new HttpRequest(request.getString(HttpChannelEncoder.REQUEST_LINE), request.getAttribute(HttpChannelEncoder.HEADERS, Headers.class));
                    httpRequest.setBody(request.getPayload());

                    final ResteasyHttpResponseWrapper restEasyHttpResponse = new ResteasyHttpResponseWrapper(response, this);
                    final ResteasyHttpRequestWrapper restEasyHttpRequest = new ResteasyHttpRequestWrapper(httpRequest, restEasyHttpResponse, (SynchronousDispatcher) contextBuilder.getDeployment().getDispatcher(), contextPath);

                    // add them to the context
                    ResteasyProviderFactory.getContextDataMap().put(org.jboss.resteasy.spi.HttpRequest.class, restEasyHttpRequest);
                    ResteasyProviderFactory.getContextDataMap().put(org.jboss.resteasy.spi.HttpResponse.class, restEasyHttpResponse);

                    // provide a SSE event sink
                    final SseEventSink sseEventSink = new SseEventOutputImpl(new SseEventProvider());
                    ResteasyProviderFactory.getContextDataMap().put(SseEventSink.class, sseEventSink);

                    final ResourceInvoker invoker = ((ResourceMethodRegistry) contextBuilder.getDeployment().getRegistry()).getResourceInvoker(restEasyHttpRequest);

                    if (invoker != null) {
                        restEasyHttpRequest.setAttribute(invoker.getClass().getName(), invoker);

                        // TODO set path parameters and query parameters on resteasyrequest uriinfo
                        ParamsParser.parse(restEasyHttpRequest, invoker.getMethod(), contextBuilder.getPath());

                        final ResourceClass resourceClass = new DefaultResourceClass(invoker.getMethod().getDeclaringClass(), restEasyHttpRequest.getUri().getPath());
                        final POJOResourceFactory rf = new POJOResourceFactory(resourceClass);
                        final ResourceMethodInvoker methodInvoker = new ResourceMethodInvoker(new DefaultResourceMethod(resourceClass, invoker.getMethod(), invoker.getMethod()), contextBuilder.getDeployment().getInjectorFactory(), rf, contextBuilder.getDeployment().getProviderFactory());
                        restEasyHttpRequest.setAttribute(ResourceMethodInvoker.class.getName(), methodInvoker);
                    }

                    contextBuilder.getDeployment().getDispatcher().invoke(restEasyHttpRequest, restEasyHttpResponse);

                    // write headers and body to HttpResponse
                    restEasyHttpResponse.mergeToResponse();

                    String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

                    if (contentType == null) {
                        contentType = determineContentType(response.getBody());
                        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
                    }

                    // fix a bug in Resteasy which returns a 404 for SSE
                    if (contentType != null && contentType.equals(MediaType.SERVER_SENT_EVENTS) && response.getStatus() == HttpStatus.NOT_FOUND && response.getBody() != null) {
                        response.withStatus(HttpStatus.SUCCESS);
                    }

                    final ByteBuffer buffer = response.getBody();
                    buffer.mark();
                    final byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    buffer.reset();

                    logger.info(httpRequest.getRequestLine() + " -> " + new String(bytes));

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


    /**
     * Sets the {@link HttpResponse}.
     * 
     * @param response the {@link HttpResponse}
     */
    public void setHttpResponse(final HttpResponse response) {
        this.response = response;
    }


    /**
     * The default implementation ignores the {@link CompletionHandler} and signals to close the
     * connection.
     * 
     * @param handler a handler (ignored)
     * @param length the amount of bytes written
     * @return {@link ChannelAction#CLOSE_ALL}
     */
    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
        return ChannelAction.CLOSE_ALL;
    }


    /**
     * The default implementation ignores the {@link CompletionHandler} and signals to close the
     * connection.
     * 
     * @param handler a handler (ignored)
     * @param t the exception
     * @return {@link ChannelAction#CLOSE_ALL}
     */
    @Override
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    /**
     * The default implementation signals to close the connection.
     * 
     * @param t the exception
     * @return {@link ChannelAction#CLOSE_ALL}
     */
    @Override
    public ChannelAction onReadError(final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    /**
     * The default implementation will tell the connection that it is ready to write.
     * 
     * @throws IOException if something goes wrong
     */
    @Override
    public void handleClosedInput() throws IOException {
        session.getChannelProcessor().getChannel().keyFor(session.getChannelProcessor().getSelector()).interestOpsOr(SelectionKey.OP_WRITE);
    }

}
