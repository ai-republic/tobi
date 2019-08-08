package com.airepublic.tobi.feature.mp.faulttolerance;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Implementation to intercept {@link Asynchronous} annotations.
 * 
 * @see {@link Asynchronous} annotation for details on the specified mechanism
 * @author Torsten Oltmanns
 *
 */
@Asynchronous
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class AsynchronousInterceptor implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * Intercepts the {@link Asynchronous} annotation and executes the method in a
     * {@link ForkJoinPool}.
     * 
     * @param context the {@link InvocationContext}
     * @return the result of the method (a {@link CompletableFuture}
     * @throws Throwable if execution fails
     */
    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Throwable {
        final Class<?> returnType = context.getMethod().getReturnType();

        if (!CompletionStage.class.isAssignableFrom(returnType) && !Future.class.isAssignableFrom(returnType)) {
            throw new FaultToleranceDefinitionException("Asynchronous annotated method must have a return type of CompletionStage or Future!");
        }

        // asynchronous multi-threaded execution is managed through Bulkhead thread-pool
        ExecutorService executor = (ExecutorService) context.getContextData().get(ExecutorService.class.getName());

        if (executor == null) {
            executor = ForkJoinPool.commonPool();
        }

        final CompletableFuture<Object> future = new CompletableFuture<>();

        if (CompletionStage.class.isAssignableFrom(returnType)) {
            future.completeAsync(() -> {
                try {
                    final CompletionStage<Object> stage = (CompletionStage<Object>) context.proceed();
                    return stage.toCompletableFuture().get();
                } catch (final Exception e) {
                    future.completeExceptionally(e);
                    return null;
                }
            }, executor);

        } else {
            future.completeAsync(() -> {
                try {
                    final Future<Object> stage = (Future<Object>) context.proceed();
                    return stage.get();
                } catch (final Exception e) {
                    future.completeExceptionally(e.getCause());
                    return null;
                }
            }, executor);
        }

        return future;
    }

}
