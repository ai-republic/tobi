package com.airepublic.tobi.feature.mp.faulttolerance;

import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Implementation to intercept {@link Timeout} annotations.
 * 
 * @see {@link Timeout} annotation for details on the specified mechanism
 * @author Torsten Oltmanns
 *
 */
@Timeout
@Interceptor
public class TimeoutInterceptor implements Serializable {
    private static final long serialVersionUID = 1L;


    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Throwable {
        final Timeout timeout = context.getMethod().getAnnotation(Timeout.class);
        validate("Timeout.value", timeout.value(), 0L);

        final long value = timeout.value();
        final ChronoUnit unit = timeout.unit();

        if (value > 0L) {
            final CompletableFuture<Object> future = new CompletableFuture<>();
            future.orTimeout(value, TimeUnit.of(unit));
            future.completeAsync(() -> {
                try {
                    return context.proceed();
                } catch (final Exception e) {
                    future.completeExceptionally(e);
                    return null;
                }
            });

            try {
                final Object result = future.get();
                return result;
            } catch (final ExecutionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    throw new org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException(e.getCause().getMessage(), e.getCause());
                }

                throw e.getCause();
            }
        } else {
            return context.proceed();
        }
    }


    private void validate(final String field, final long value, final long minValue) {
        if (value < minValue) {
            throw new FaultToleranceDefinitionException(field + " must be >= " + minValue + "!");
        }
    }

}
