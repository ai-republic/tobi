package com.airepublic.microprofile.plugin.http.sse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.net.ssl.SSLEngine;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IRequest;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.SessionConstants;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.plugin.http.sse.api.SseEvent;
import com.airepublic.microprofile.plugin.http.sse.api.SseProducer;
import com.airepublic.microprofile.plugin.http.sse.api.SseService;
import com.airepublic.microprofile.util.http.common.HttpRequest;

public class SseOutboundIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private IServerSession session;
    @Inject
    private SseService sseService;
    private final AtomicBoolean isHandshakeRead = new AtomicBoolean(false);
    private Object serviceObject;
    private Method serviceMethod;
    private long delayInMs = 0L;
    private long times = 0L;
    private long maxTimes = -1L;


    @Override
    public IServerSession getSession() {
        return session;
    }


    @Override
    public void setSession(final IServerSession session) {
        this.session = session;
    }


    @Override
    public ChannelAction consume(final IRequest request) throws IOException {
        if (isHandshakeRead.compareAndSet(false, true)) {
            try {
                final HttpRequest httpRequest = (HttpRequest) request;

                if (serviceMethod == null || serviceObject == null) {
                    throw new IOException("URI path " + httpRequest.getPath() + " was not be mapped to a SSE method!");
                }

                if (serviceMethod.isAnnotationPresent(SseProducer.class)) {
                    final SseProducer annotation = serviceMethod.getAnnotation(SseProducer.class);
                    delayInMs = annotation.unit().toMillis(annotation.delay());
                    maxTimes = annotation.maxTimes();
                }

                final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_SSL_ENGINE, SSLEngine.class);

                // respond to the handshake request
                sseService.handshake(session.getChannel(), sslEngine);

                // register to produce events only
                session.getChannel().keyFor(session.getChannelProcessor().getSelector()).interestOps(SelectionKey.OP_WRITE);

                // close input channel
                return ChannelAction.CLOSE_INPUT;
            } catch (final Exception e) {
                throw new IOException("Error in request URI syntax!", e);
            }
        }

        throw new IllegalCallerException("Input should be closed for any further calls!");

    }


    @Override
    public void produce() throws IOException {
        try {
            final Object result = serviceMethod.invoke(serviceObject, new Object[0]);

            if (result instanceof SseEvent) {
                final SseEvent event = (SseEvent) result;
                session.addToWriteBuffer(sseService.encode(event));
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Could not invoke SSE outbound producer method: " + serviceMethod, e);
        }
    }


    @Override
    public ChannelAction onReadError(final Throwable t) {
        return ChannelAction.CLOSE_INPUT;
    }


    @Override
    public void handleClosedInput() throws IOException {
    }


    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {

        if (maxTimes == -1 || times < maxTimes) {
            times++;

            try {
                Thread.sleep(delayInMs);
                session.getChannel().keyFor(session.getChannelProcessor().getSelector()).interestOps(SelectionKey.OP_WRITE);
                session.getChannelProcessor().getSelector().wakeup();
                return ChannelAction.CLOSE_INPUT;
            } catch (final InterruptedException e) {
                logger.log(Level.WARNING, "SSE for outbound events was interrupted!", e);
                return ChannelAction.CLOSE_ALL;
            }
        } else {
            return ChannelAction.CLOSE_ALL;
        }
    }


    @Override
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    @Override
    public void onSessionClose() {
    }


    public void setServiceMethod(final Method serviceMethod) {
        this.serviceMethod = serviceMethod;
    }


    public void setServiceObject(final Object serviceObject) {
        this.serviceObject = serviceObject;
    }
}
