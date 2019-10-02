package com.airepublic.tobi.feature.mp.faulttolerance;

import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Implementation to intercept {@link CircuitBreaker} annotations.
 * 
 * See {@link CircuitBreaker} annotation for details on the specified mechanism
 * 
 * @author Torsten Oltmanns
 *
 */
@CircuitBreaker
@Interceptor
public class CircuitBreakerInterceptor implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<Object, CircuitBreakerContext> openCircuits = new HashMap<>();

    enum CircuitBreakerState {
        CLOSED, OPEN, CLOSING
    };

    /**
     * Context information for the open circuits.
     * 
     * @author Torsten Oltmanns
     *
     */
    class CircuitBreakerContext {
        private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        private final AtomicLong callCount = new AtomicLong(0L);
        private final AtomicLong failureCount = new AtomicLong(0L);
        private final AtomicLong lastTry = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger successCount = new AtomicInteger(0);
        private Throwable error = null;
    }


    /**
     * Intercepts the {@link CircuitBreaker} annotation.
     * 
     * @param context the {@link InvocationContext}
     * @return the result of the method
     * @throws Throwable if execution fails
     */
    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Throwable {
        Object result = null;
        final CircuitBreaker circuitBreaker = context.getMethod().getAnnotation(CircuitBreaker.class);
        validate("CircuitBreaker.delay", circuitBreaker.delay(), 0L);
        validate("CircuitBreaker.requestVolumeThreshold", circuitBreaker.requestVolumeThreshold(), 1L);
        validateBetween("CircuitBreaker.failureRatio", circuitBreaker.failureRatio(), 0d, 1d);
        validate("CircuitBreaker.successThreshold", circuitBreaker.successThreshold(), 1L);

        CircuitBreakerContext circuitBreakerContext = openCircuits.get(context.getTarget());

        // check if we need to create a context
        if (circuitBreakerContext == null) {
            circuitBreakerContext = new CircuitBreakerContext();
            openCircuits.put(context.getTarget(), circuitBreakerContext);
        }

        boolean retry = false;

        do {
            retry = false;

            // perform the real call only if the circuit is closed or a retry is attempted
            if (circuitBreakerContext == null || circuitBreakerContext.state.get() == CircuitBreakerState.CLOSED || circuitBreakerContext.state.get() == CircuitBreakerState.CLOSING) {
                try {
                    // set call metrics
                    circuitBreakerContext.callCount.incrementAndGet();
                    circuitBreakerContext.lastTry.set(System.currentTimeMillis());

                    result = context.proceed();

                    // if circuit is open increment the success count and check if the circuit can
                    // be closed
                    if (circuitBreakerContext.state.get() == CircuitBreakerState.CLOSING) {
                        circuitBreakerContext.successCount.incrementAndGet();
                        checkToCloseCircuit(circuitBreaker, circuitBreakerContext, context);
                    } else {
                        // if circuit is closed reset the call count
                        circuitBreakerContext.callCount.set(0L);
                    }
                } catch (final Throwable t) {

                    // check if one of the defined exceptions matches the error
                    if (Stream.of(circuitBreaker.failOn()).anyMatch(c -> c.isAssignableFrom(t.getClass()))) {

                        // set failure metrics
                        circuitBreakerContext.error = t;
                        circuitBreakerContext.failureCount.incrementAndGet();
                        circuitBreakerContext.successCount.set(0);

                        // check if the circuit needs to be opened
                        checkToOpenCircuit(circuitBreaker, circuitBreakerContext);

                        throw t;
                    } else if (circuitBreakerContext.state.get() == CircuitBreakerState.CLOSING) {
                        circuitBreakerContext.successCount.incrementAndGet();
                        checkToCloseCircuit(circuitBreaker, circuitBreakerContext, context);
                    }
                }
            } else {
                // if circuit is open check delay
                final long delay = circuitBreaker.delay();
                final ChronoUnit delayUnit = circuitBreaker.delayUnit();
                final long realDelay = Duration.of(delay, delayUnit).toMillis();

                // if the delay is over, retry the actual call
                if (System.currentTimeMillis() >= circuitBreakerContext.lastTry.get() + realDelay) {
                    circuitBreakerContext.state.set(CircuitBreakerState.CLOSING);
                    retry = true;
                } else {
                    // otherwise throw the last known error
                    throw circuitBreakerContext.error;
                }
            }
        } while (retry);

        return result;
    }


    /**
     * Checks if the circuit-breaker needs to change to the open state.
     * 
     * @param circuitBreaker the {@link CircuitBreaker}
     * @param circuitBreakerContext the {@link CircuitBreakerContext}
     */
    private void checkToOpenCircuit(final CircuitBreaker circuitBreaker, final CircuitBreakerContext circuitBreakerContext) {
        if (circuitBreakerContext.state.get() == CircuitBreakerState.CLOSED || circuitBreakerContext.state.get() == CircuitBreakerState.CLOSING) {
            if (circuitBreakerContext.callCount.get() >= circuitBreaker.requestVolumeThreshold()) {
                if (circuitBreakerContext.callCount.get() / (double) circuitBreakerContext.failureCount.get() >= circuitBreaker.failureRatio()) {
                    circuitBreakerContext.state.set(CircuitBreakerState.OPEN);
                }
            }
        }
    }


    /**
     * Checks if the circuit-breaker can be changed to the closed state.
     * 
     * @param circuitBreaker the {@link CircuitBreaker}
     * @param circuitBreakerContext the {@link CircuitBreakerContext}
     * @param context the {@link InvocationContext}
     */
    private void checkToCloseCircuit(final CircuitBreaker circuitBreaker, final CircuitBreakerContext circuitBreakerContext, final InvocationContext context) {
        // check if the circuit is ready to be fully closed
        if (circuitBreakerContext.state.get() == CircuitBreakerState.CLOSING && circuitBreakerContext.successCount.get() >= circuitBreaker.successThreshold()) {
            // circuit can be closed so we just remove the context
            openCircuits.remove(context.getTarget());
        }
    }


    /**
     * Validates the if the field is below the minimum value.
     * 
     * @param field the field
     * @param value the current value
     * @param minValue the minimum value
     */
    private void validate(final String field, final long value, final long minValue) {
        if (value < minValue) {
            throw new FaultToleranceDefinitionException(field + " must be >= " + minValue + "!");
        }
    }


    /**
     * Validates the if the field is between the minimum value and maximum value.
     * 
     * @param field the field
     * @param value the current value
     * @param minValue the minimum value
     * @param maxValue the maximum value
     */
    private void validateBetween(final String field, final double value, final double minValue, final double maxValue) {
        if (value < minValue || value > maxValue) {
            throw new FaultToleranceDefinitionException(field + " must be between " + minValue + " and " + maxValue + "!");
        }

    }

}
