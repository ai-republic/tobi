package com.airepublic.microprofile.plugin.http.sse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.util.http.common.AsyncHttpReader;
import com.airepublic.microprofile.util.http.common.BufferUtil;
import com.airepublic.microprofile.util.http.common.Headers;
import com.airepublic.microprofile.util.http.common.HttpRequest;

@SessionScoped
public class SseInboundIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private final AsyncHttpReader httpReader = new AsyncHttpReader();
    @Inject
    private IServerSession session;
    private final AtomicBoolean isHandshakeDone = new AtomicBoolean(false);
    private final AtomicBoolean isResponseDone = new AtomicBoolean(false);
    private SseEventSourceImpl sseEventSource;
    private final SseAsyncEventReader eventReader = new SseAsyncEventReader();


    @Override
    public ChannelAction consume(final ByteBuffer buffer) throws IOException {
        boolean newEvents = false;

        if (!isResponseDone.get()) {
            if (httpReader.receiveBuffer(buffer)) {
                isResponseDone.set(true);

                try {
                    final ByteBuffer body = httpReader.getHttpResponse().getBody();

                    if (body.limit() > 0) {
                        body.position(0);
                        newEvents = eventReader.receiveBuffer(BufferUtil.copyRemainingBuffer(body));
                    }
                } catch (final Exception e) {
                    sseEventSource.onError(e);
                }
            }
        } else {
            newEvents = eventReader.receiveBuffer(BufferUtil.copyRemainingBuffer(buffer));
        }

        if (newEvents) {
            InboundSseEventImpl event = eventReader.poll();

            while (event != null) {
                sseEventSource.onEvent(event);
                event = eventReader.poll();
            }
        }

        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public void produce() throws IOException {
        if (isHandshakeDone.compareAndSet(false, true)) {
            doHandshake(session.getChannel());
        }
    }


    protected void doHandshake(final SocketChannel channel) throws IOException {
        sseEventSource = session.getAttribute(SsePlugin.SSE_SESSION_EVENTSOURCE, SseEventSourceImpl.class);

        final Headers headers = new Headers();
        headers.add(Headers.HOST, sseEventSource.getUri().getHost());
        headers.add(Headers.ACCEPT, "*/*");

        final HttpRequest request = new HttpRequest(sseEventSource.getUri(), headers);
        request.withMethod("GET").withVersion("HTTP/1.1");
        session.addToWriteBuffer(request.getHeaderBuffer());
    }


    @Override
    public ChannelAction onReadError(final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    @Override
    public void handleClosedInput() throws IOException {
    }


    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
        session.getSelectionKey().interestOps(SelectionKey.OP_READ);

        // output not needed after handshake
        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    @Override
    public void onSessionClose() {
        final Long reconnectDelay = session.getAttribute(SsePlugin.SSE_SESSION_EVENTSOURCE_RECONNECTION_DELAY, Long.class);

        ForkJoinPool.commonPool().execute(() -> restart(sseEventSource, reconnectDelay));
    }


    public void restart(final SseEventSourceImpl eventSource, final long delayInMs) {
        try {
            Thread.sleep(delayInMs);
        } catch (final InterruptedException e) {
        }

        eventSource.open();
    }
}
