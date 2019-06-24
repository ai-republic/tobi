package com.airepublic.microprofile.feature.mp.faulttolerance;

import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Implementation to intercept {@link Retry} annotations.
 * 
 * @see {@link Retry} annotation for details on the specified mechanism
 * @author Torsten Oltmanns
 *
 */
@Retry
@Interceptor
public class RetryInterceptor implements Serializable {
    private static final long serialVersionUID = 1L;


    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        final long startTime = System.currentTimeMillis();
        Object result = null;
        int failCount = 0;
        Throwable error = null;
        boolean done = false;
        final Retry retry = context.getMethod().getAnnotation(Retry.class);
        validate("Retry.delay", retry.delay(), 0);
        validate("Retry.maxDuration", retry.maxDuration(), 0);

        do {
            error = null;
            done = false;

            try {
                result = context.proceed();
            } catch (final Throwable t) {
                error = t;

                // check retryOn
                if (retry.retryOn() != null) {
                    if (Stream.of(retry.retryOn()).filter(c -> Error.class.isAssignableFrom(c) || Exception.class.isAssignableFrom(c)).anyMatch(c -> c.isAssignableFrom(t.getClass()))) {
                        done = false;
                    }
                }

                // check abortOn
                if (retry.abortOn() != null) {
                    if (Stream.of(retry.abortOn()).filter(c -> Error.class.isAssignableFrom(c) || Exception.class.isAssignableFrom(c)).anyMatch(c -> c.isAssignableFrom(t.getClass()))) {
                        done = true;
                    }
                }

                // check maxRetries
                if (retry.maxRetries() != -1) {
                    if (failCount >= retry.maxRetries()) {
                        done = true;
                    } else {
                        failCount++;
                    }
                }

                // check delay and jitter
                final long delay = retry.delay();
                final ChronoUnit delayUnit = retry.delayUnit();
                final long jitterSpread = retry.jitter() > 0 ? retry.jitter() : 0L;
                final ChronoUnit jitterUnit = retry.jitterDelayUnit();
                final long jitter = Math.random() >= 0.5d ? jitterSpread : -jitterSpread;

                final long realDelay = Duration.of(delay, delayUnit).toMillis() + Duration.of(jitter, jitterUnit).toMillis();
                final long maxDuration = Duration.of(retry.maxDuration(), retry.durationUnit()).toMillis();

                if (System.currentTimeMillis() - startTime + realDelay > maxDuration) {
                    done = true;
                } else if (realDelay > 0L) {
                    Thread.sleep(realDelay);
                }
            }
        } while (!done);

        if (error != null) {
            throw new RuntimeException(error);
        }

        return result;
    }


    private void validate(final String field, final long value, final long minValue) {
        if (value < minValue) {
            throw new FaultToleranceDefinitionException(field + " must be >= " + minValue + "!");
        }

    }

}
