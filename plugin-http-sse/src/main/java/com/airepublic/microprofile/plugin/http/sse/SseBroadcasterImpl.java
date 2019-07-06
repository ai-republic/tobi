package com.airepublic.microprofile.plugin.http.sse;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

public class SseBroadcasterImpl implements SseBroadcaster {

    @Override
    public void onError(final BiConsumer<SseEventSink, Throwable> onError) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onClose(final Consumer<SseEventSink> onClose) {
        // TODO Auto-generated method stub

    }


    @Override
    public void register(final SseEventSink sseEventSink) {
        // TODO Auto-generated method stub

    }


    @Override
    public CompletionStage<?> broadcast(final OutboundSseEvent event) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

}
