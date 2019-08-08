package com.airepublic.tobi.testsuite.faulttolerance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Test class to test the use of the {@link Timeout} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class TimeoutTestClass {
    int counter = 0;
    List<Long> callTimes = new ArrayList<>();


    /**
     * Method to test simple {@link Timeout} usage.
     */
    @Timeout(1000L)
    public void timeout() {
        try {
            Thread.sleep(1100L);
        } catch (final InterruptedException e) {
        }
    }


    /**
     * Method to test {@link Timeout} usage with exception.
     */
    @Timeout(1000L)
    public void timeoutWithIOException() throws IOException {
        throw new IOException();
    }

}
