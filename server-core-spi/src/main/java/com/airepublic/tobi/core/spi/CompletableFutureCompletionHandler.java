package com.airepublic.tobi.core.spi;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

/**
 * A {@link CompletionHandler} implementation that holds a {@link CompletableFuture} to await and
 * provide the result of an IO operation.
 * 
 * @author Torsten Oltmanns
 *
 * @param <V> the result type of the operation
 * @param <A> the attachment of the operation
 */
public class CompletableFutureCompletionHandler<V, A> implements CompletionHandler<V, A> {
    private final CompletableFuture<V> future;


    /**
     * Constructor.
     */
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


    /**
     * Gets the associated {@link CompletableFuture} that holds the result of the operation.
     * 
     * @return the {@link CompletableFuture}
     */
    public CompletableFuture<V> getFuture() {
        return future;
    }
}