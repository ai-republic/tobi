package com.airepublic.microprofile.core.spi;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureCompletionHandler<V, A> implements CompletionHandler<V, A> {
    private final CompletableFuture<V> future;


    public CompletableFutureCompletionHandler() {
        future = new CompletableFuture<>();
    }


    @Override
    public void completed(final V result, final A attachment) {
        future.complete(result);
    }


    @Override
    public void failed(final Throwable ex, final A attachment) {
        future.completeExceptionally(ex);
    }


    public CompletableFuture<V> getFuture() {
        return future;
    }
}