package com.airepublic.microprofile.plugin.http.sse;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RUNTIME)
@Target(METHOD)
public @interface SseRepeat {
    /**
     * The delay before the call to the producer method will be repeated. The default is 1. The
     * effective delay is calculated using the {@link SseRepeat#unit()} parameter.
     * 
     * @return the delay
     */
    long delay() default 1L;


    /**
     * The {@link TimeUnit} for the {@link SseRepeat#delay()} parameter. Default is
     * {@link TimeUnit#SECONDS}.
     * 
     * @return the {@link TimeUnit}
     */
    TimeUnit unit() default TimeUnit.SECONDS;


    /**
     * Specified how often the SSE producer method should be called before the connection is closed.
     * Default is 1 time. A value of -1 means forever.
     * 
     * @return the maximum number of times the producer method can be called before the connection
     *         is closed
     */
    long maxTimes() default 1L;
}
