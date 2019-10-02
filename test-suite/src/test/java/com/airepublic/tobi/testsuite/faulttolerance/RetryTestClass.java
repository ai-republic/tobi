package com.airepublic.tobi.testsuite.faulttolerance;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * Test class to test the use of the {@link Retry} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class RetryTestClass {
    int counter = 0;
    List<Long> callTimes = new ArrayList<>();


    /**
     * Method to test the {@link Retry} annotation abortOn parameter.
     * 
     * @param throwable the exception to throw
     * @throws Throwable the passed exception
     */
    @Retry(abortOn = IOException.class)
    public void retryAbortOn(final Throwable throwable) throws Throwable {
        if (throwable != null) {
            counter++;
            throw throwable;
        }
    }


    /**
     * Method to test the {@link Retry} annotation delay parameter.
     */
    @Retry(delay = 1000L, jitter = 0L)
    public void retryDelay() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    /**
     * Method to test the {@link Retry} annotation delay with delayUnit parameter.
     */
    @Retry(delay = 1L, delayUnit = ChronoUnit.SECONDS, jitter = 0L)
    public void retryDelayWithUnit() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    /**
     * Method to test the {@link Retry} annotation delay with jitter parameter.
     */
    @Retry(delay = 1L, delayUnit = ChronoUnit.SECONDS, jitter = 100L)
    public void retryDelayWithJitter() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    /**
     * Method to test the {@link Retry} annotation delay with jitter and jitterDelayUnit parameter.
     */
    @Retry(delay = 1L, delayUnit = ChronoUnit.SECONDS, jitter = 1L, jitterDelayUnit = ChronoUnit.SECONDS)
    public void retryDelayWithJitterWithUnit() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    /**
     * Method to test the {@link Retry} annotation delay and maxDuration parameter.
     */
    @Retry(delay = 400L, maxDuration = 1000L, jitter = 0L)
    public void retryMaxDuration() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    /**
     * Method to test the {@link Retry} annotation delay and maxDuration with durationUnit
     * parameter.
     */
    @Retry(delay = 400L, maxDuration = 1L, durationUnit = ChronoUnit.SECONDS, jitter = 0L)
    public void retryMaxDurationWithUnit() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    /**
     * Method to test the {@link Retry} annotation maxRetries parameter.
     */
    @Retry(maxRetries = 2)
    public void retryMaxRetries() {
        counter++;
        throw new RuntimeException();
    }

}
