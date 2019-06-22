package com.airepublic.microprofile.faulttolerance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Implementation to intercept {@link Bulkhead} annotations.
 * 
 * @see {@link Bulkhead} annotation for details on the specified mechanism
 * @author Torsten Oltmanns
 *
 */
@Bulkhead
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 90)
public class BulkheadInterceptor {
    private final Map<Object, Semaphore> semaphores = new HashMap<>();
    private final Map<Object, ThreadPoolExecutor> forkJoinPools = new HashMap<>();


    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Throwable {
        final Bulkhead bulkhead = context.getMethod().getAnnotation(Bulkhead.class);
        final int value = bulkhead.value();
        final int waitingTaskQueue = bulkhead.waitingTaskQueue();

        validate("Bulkhead.value", value, 0);
        validate("Bulkhead.waitingTaskQueue", waitingTaskQueue, 0);

        Object result = null;

        // if annotation Asynchronous is present, run in a separate thread
        if (context.getMethod().isAnnotationPresent(Asynchronous.class)) {
            ThreadPoolExecutor executor = forkJoinPools.get(context.getTarget());

            if (executor == null) {
                executor = new ThreadPoolExecutor(value, value, 10000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(waitingTaskQueue));
                // forkJoinPool = new ForkJoinPool(value,
                // ForkJoinPool.defaultForkJoinWorkerThreadFactory, (UncaughtExceptionHandler) null,
                // true, value, value, value, (t) -> (value <= value + waitingTaskQueue), 10000L,
                // TimeUnit.MILLISECONDS);
                forkJoinPools.put(context.getTarget(), executor);
            }

            context.getContextData().put(ExecutorService.class.getName(), executor);
            result = context.proceed();
        } else {
            // otherwise restrict the calls via semaphore
            Semaphore semaphore = semaphores.get(context.getTarget());

            if (semaphore == null) {
                semaphore = new Semaphore(value, true);
                semaphores.put(context.getTarget(), semaphore);
            }

            semaphore.acquire();
            result = context.proceed();
            semaphore.release();
        }

        return result;
    }


    private void validate(final String field, final long value, final long minValue) {
        if (value < minValue) {
            throw new FaultToleranceDefinitionException(field + " must be >= " + minValue + "!");
        }

    }
}
