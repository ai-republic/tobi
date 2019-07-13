package com.airepublic.microprofile.plugin.http.sse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import com.airepublic.microprofile.core.SessionContainer;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.SessionAttributes;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.util.http.common.SessionConstants;

/**
 * Implementation of the {@link SseEventSource}.
 * 
 * @author Torsten Oltmanns
 *
 */
@ApplicationScoped
public class SseEventSourceImpl implements SseEventSource {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private final Set<SseInboundEventConsumer> consumers = new HashSet<>();
    private final AtomicBoolean open = new AtomicBoolean(false);
    private IServerModule module;
    private WebTarget endpoint;
    private Long reconnectionDelay = 5000L;
    private Method method;


    @Override
    public void register(final Consumer<InboundSseEvent> onEvent) {
        register(onEvent, null, null);
    }


    @Override
    public void register(final Consumer<InboundSseEvent> onEvent, final Consumer<Throwable> onError) {
        register(onEvent, onError, null);
    }


    @Override
    public void register(final Consumer<InboundSseEvent> onEvent, final Consumer<Throwable> onError, final Runnable onComplete) {
        consumers.add(new SseInboundEventConsumer(onEvent, onError, onComplete));
    }


    @Override
    public void open() {
        int checkPort = endpoint.getUri().getScheme().equals("https") ? 443 : 80;

        if (endpoint.getUri().getPort() > 0) {
            checkPort = endpoint.getUri().getPort();
        }

        final int port = checkPort;
        final Supplier<SocketChannel> supplier = () -> {

            SocketChannel channel;

            try {
                final SocketAddress remote = new InetSocketAddress(endpoint.getUri().getHost(), port);
                channel = SocketChannel.open();
                channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                channel.configureBlocking(false);
                channel.connect(remote);

                while (!channel.finishConnect()) {
                }
            } catch (final IOException e) {
                throw new RuntimeException("Failed to create socket channel!", e);
            }

            return channel;
        };

        open.set(true);

        final SessionAttributes attributes = new SessionAttributes();
        attributes.set(SsePlugin.SSE_SESSION_EVENTSOURCE, this);
        attributes.set(SsePlugin.SSE_SESSION_EVENTSOURCE_ENDPOINT, endpoint);
        attributes.set(SsePlugin.SSE_SESSION_EVENTSOURCE_RECONNECTION_DELAY, reconnectionDelay);
        attributes.set(IServerSession.SESSION_IO_HANDLER_CLASS, SseInboundIOHandler.class);
        attributes.set(IServerSession.SESSION_IS_SECURE, endpoint.getUri().getScheme().equals("https"));
        attributes.set(SessionConstants.SESSION_SSL_CLIENT_HOST, endpoint.getUri().getHost());
        attributes.set(SessionConstants.SESSION_SSL_CLIENT_PORT, port);

        final SessionContainer sessionContainer = CDI.current().select(SessionContainer.class).get();
        sessionContainer.startSession(module, supplier, attributes, true);

    }


    void onEvent(final InboundSseEvent event) {
        consumers.forEach(consumer -> {
            if (consumer.getOnEvent() != null) {
                consumer.getOnEvent().accept(event);
            }
        });
    }


    void onError(final Throwable t) {
        consumers.forEach(consumer -> {
            if (consumer.getOnError() != null) {
                consumer.getOnError().accept(t);
            }
        });
    }


    void onComplete() {
        consumers.forEach(consumer -> {
            if (consumer.getOnComplete() != null) {
                consumer.getOnComplete().run();
            }
        });
    }


    @Override
    public boolean isOpen() {
        return open.get();
    }


    @Override
    public boolean close(final long timeout, final TimeUnit unit) {
        open.set(false);

        synchronized (this) {
            final long timeoutMillis = unit.toMillis(timeout);

            try {
                wait(timeoutMillis);
            } catch (final InterruptedException e) {
                Thread.interrupted();
                return false;
            }
        }
        return true;
    }


    public void setEndpoint(final WebTarget endpoint) {
        this.endpoint = endpoint;
    }


    public void setReconnectionDelay(final long reconnectionDelay) {
        this.reconnectionDelay = reconnectionDelay;
    }


    public void setModule(final IServerModule module) {
        this.module = module;
    }


    public Method getMethod() {
        return method;
    }


    public void setMethod(final Method method) {
        this.method = method;
    }


    URI getUri() {
        return endpoint.getUri();
    }

}
