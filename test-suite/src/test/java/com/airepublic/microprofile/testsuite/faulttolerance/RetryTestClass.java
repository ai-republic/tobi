package com.airepublic.microprofile.testsuite.faulttolerance;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;

public class RetryTestClass {
    int counter = 0;
    List<Long> callTimes = new ArrayList<>();


    @Retry(abortOn = IOException.class)
    public void retryAbortOn(final Throwable throwable) throws Throwable {
        if (throwable != null) {
            counter++;
            throw throwable;
        }
    }


    @Retry(delay = 1000L, jitter = 0L)
    public void retryDelay() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    @Retry(delay = 1L, delayUnit = ChronoUnit.SECONDS, jitter = 0L)
    public void retryDelayWithUnit() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    @Retry(delay = 1L, delayUnit = ChronoUnit.SECONDS, jitter = 100L)
    public void retryDelayWithJitter() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    @Retry(delay = 1L, delayUnit = ChronoUnit.SECONDS, jitter = 1L, jitterDelayUnit = ChronoUnit.SECONDS)
    public void retryDelayWithJitterWithUnit() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    @Retry(delay = 400L, maxDuration = 1000L, jitter = 0L)
    public void retryMaxDuration() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    @Retry(delay = 400L, maxDuration = 1L, durationUnit = ChronoUnit.SECONDS, jitter = 0L)
    public void retryMaxDurationWithUnit() {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new RuntimeException();
    }


    @Retry(maxRetries = 2)
    public void retryMaxRetries() {
        counter++;
        throw new RuntimeException();
    }

}
