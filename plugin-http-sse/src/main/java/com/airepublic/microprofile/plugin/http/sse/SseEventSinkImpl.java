package com.airepublic.microprofile.plugin.http.sse;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEvent;
import javax.ws.rs.sse.SseEventSink;

import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.util.http.common.CompletableFutureCompletionHandler;

/**
 * Implementation of the {@link SseEventSink}.
 * 
 * @author Torsten Oltmanns
 *
 */
@SessionScoped
public class SseEventSinkImpl implements SseEventSink, Serializable {
    private static final long serialVersionUID = 1L;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    @Inject
    private IServerSession session;


    @Override
    public boolean isClosed() {
        return closed.get();
    }


    @Override
    public CompletionStage<?> send(final OutboundSseEvent event) {
        if (!isClosed()) {
            final StringBuffer buffer = new StringBuffer();

            if (event.getName() != null) {
                buffer.append("event:").append(event.getName()).append("\n");
            }

            if (event.getId() != null) {
                buffer.append("id:").append(event.getId()).append("\n");
            }

            if (event.getComment() != null) {
                buffer.append(":").append(event.getComment()).append("\n");
            }

            if (event.getReconnectDelay() != SseEvent.RECONNECT_NOT_SET) {
                buffer.append("retry:").append(event.getReconnectDelay()).append("\n");
            }

            buffer.append("data:").append(event.getData()).append("\n");
            buffer.append("\n");

            final CompletableFutureCompletionHandler<Void, OutboundSseEvent> handler = new CompletableFutureCompletionHandler<>();
            session.addToWriteBuffer(handler, ByteBuffer.wrap(buffer.toString().getBytes()));

            return handler.getFuture();
        }

        return null;
    }


    @Override
    public void close() {
        closed.set(true);
        session.close();
    }

}
