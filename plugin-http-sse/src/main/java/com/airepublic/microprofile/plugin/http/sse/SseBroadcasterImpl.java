package com.airepublic.microprofile.plugin.http.sse;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

/**
 * Implementation of the {@link SseBroadcaster} API.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SseBroadcasterImpl implements SseBroadcaster {
    private final Set<SseEventSink> sinks = new HashSet<>();
    private BiConsumer<SseEventSink, Throwable> onErrorConsumer;
    private Consumer<SseEventSink> onCloseConsumer;


    @Override
    public void onError(final BiConsumer<SseEventSink, Throwable> onError) {
        onErrorConsumer = onError;
    }


    @Override
    public void onClose(final Consumer<SseEventSink> onClose) {
        onCloseConsumer = onClose;
    }


    @Override
    public void register(final SseEventSink sseEventSink) {
        sinks.add(sseEventSink);
    }


    @Override
    public CompletionStage<?> broadcast(final OutboundSseEvent event) {
        for (final SseEventSink sink : sinks) {
            try {
                sink.send(event);
            } catch (final Exception e) {
                onErrorConsumer.accept(sink, e);
            }
        }
        return null;
    }


    @Override
    public void close() {
        sinks.forEach(sink -> {
            sink.close();
            onCloseConsumer.accept(sink);
        });
    }

}
