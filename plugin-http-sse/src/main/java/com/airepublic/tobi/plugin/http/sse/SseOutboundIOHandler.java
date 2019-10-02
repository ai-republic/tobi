package com.airepublic.tobi.plugin.http.sse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.airepublic.http.sse.api.ISseService;
import com.airepublic.http.sse.api.SseEvent;
import com.airepublic.http.sse.api.SseProducer;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.ChannelAction;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.Pair;
import com.airepublic.tobi.module.http.AbstractHttpIOHandler;
import com.airepublic.tobi.module.http.HttpRequest;
import com.airepublic.tobi.module.http.HttpResponse;

/**
 * The {@link IIOHandler} implementation for outbound SSE. It handles the IO for the object which
 * has the method annotated with {@link SseProducer}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SseOutboundIOHandler extends AbstractHttpIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private ISseService sseService;
    private final AtomicBoolean isHandshakeRead = new AtomicBoolean(false);
    private Object serviceObject;
    private Method serviceMethod;
    private long delayInMs = 0L;
    private long times = 0L;
    private long maxTimes = -1L;

    @Override
    public Pair<HttpResponse, CompletionHandler<?, ?>> getHttpResponse() throws IOException {
        if (isHandshakeRead.compareAndSet(false, true)) {
            if (serviceMethod == null || serviceObject == null) {
                final HttpRequest httpRequest = getHttpRequest();

                throw new RuntimeException("URI path " + httpRequest.getPath() + " was not be mapped to a SSE method!");
            }

            if (serviceMethod.isAnnotationPresent(SseProducer.class)) {
                final SseProducer annotation = serviceMethod.getAnnotation(SseProducer.class);
                delayInMs = annotation.unit().toMillis(annotation.delay());
                maxTimes = annotation.maxTimes();
            }

            // respond to the handshake request
            final com.airepublic.http.common.HttpResponse sseResponse = sseService.getHandshakeResponse();
            final HttpResponse response = new HttpResponse()
                    .withBody(sseResponse.getBody())
                    .withHeaders(sseResponse.getHeaders())
                    .withScheme(sseResponse.getScheme())
                    .withVersion(sseResponse.getVersion())
                    .withStatus(sseResponse.getStatus());

            // the response has already been send by the sseService
            return new Pair<>(response, null);
        }

        try {
            final Object result = serviceMethod.invoke(serviceObject, new Object[0]);

            if (result instanceof SseEvent) {
                final SseEvent event = (SseEvent) result;

                return new Pair<>(new HttpResponse().withBody(sseService.encode(event)), null);
            } else {
                return null;
            }
        } catch (final Exception e) {
            throw new IOException("Could not invoke SSE outbound producer method: " + serviceMethod, e);
        }
    }


    @Override
    public ChannelAction onReadError(final Throwable t) {
        return ChannelAction.CLOSE_INPUT;
    }


    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {

        if (maxTimes == -1 || times < maxTimes) {
            times++;

            try {
                Thread.sleep(delayInMs);
                getSession().getChannel().keyFor(getSession().getChannelProcessor().getSelector()).interestOps(SelectionKey.OP_WRITE);
                getSession().getChannelProcessor().getSelector().wakeup();
                return ChannelAction.CLOSE_INPUT;
            } catch (final InterruptedException e) {
                logger.log(Level.WARNING, "SSE for outbound events was interrupted!", e);
                return ChannelAction.CLOSE_ALL;
            }
        } else {
            return ChannelAction.CLOSE_ALL;
        }
    }


    /**
     * Sets the service method annotated with {@link SseProducer}.
     * 
     * @param serviceMethod the method annotated with {@link SseProducer}
     */
    public void setServiceMethod(final Method serviceMethod) {
        this.serviceMethod = serviceMethod;
    }


    /**
     * Sets the object instance of the class with the {@link SseProducer} service method.
     * 
     * @param serviceObject the object instance
     */
    public void setServiceObject(final Object serviceObject) {
        this.serviceObject = serviceObject;
    }
}
