package com.airepublic.tobi.testsuite.faulttolerance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

/**
 * Test class to test the use of the {@link CircuitBreaker} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class CircuitBreakerTestClass {
    int counter = 0;
    List<Long> callTimes = new ArrayList<>();


    /**
     * Method to fail and open the circuit after 2 failures.
     * 
     * @throws IOException throws {@link IOException} to open circuit
     */
    @CircuitBreaker(failOn = IOException.class, requestVolumeThreshold = 2, failureRatio = 0, delay = 1000)
    public void circuitBreakerFailOn() throws IOException {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw new IOException();
    }


    /**
     * Method to fail and open the circuit immediately and open after two successful calls.
     * 
     * @throws IOException throws {@link IOException} to open circuit or other to show success
     */
    @CircuitBreaker(failOn = IOException.class, requestVolumeThreshold = 2, failureRatio = 0, delay = 1000, successThreshold = 2)
    public void circuitBreakerSuccessThreshold(final Throwable t) throws Throwable {
        counter++;
        callTimes.add(System.currentTimeMillis());
        throw t;
    }

}
