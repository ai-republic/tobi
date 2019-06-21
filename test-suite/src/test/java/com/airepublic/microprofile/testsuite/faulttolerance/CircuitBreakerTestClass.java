package com.airepublic.microprofile.testsuite.faulttolerance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

public class CircuitBreakerTestClass {
    int counter = 0;
    List<Long> callTimes = new ArrayList<>();


    @CircuitBreaker(failOn = IOException.class, requestVolumeThreshold = 2, failureRatio = 0, delay = 1000)
    public void circuitBreakerFailOn() throws IOException {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new IOException();
    }


    @CircuitBreaker(failOn = IOException.class, requestVolumeThreshold = 2, failureRatio = 0, delay = 1000, successThreshold = 2)
    public void circuitBreakerSuccessThreshold(final Throwable t) throws Throwable {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw t;
    }

}
