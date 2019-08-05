package com.airepublic.tobi.testsuite.faulttolerance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Timeout;

public class TimeoutTestClass {
    int counter = 0;
    List<Long> callTimes = new ArrayList<>();


    @Timeout(1000L)
    public void timeout() {
        try {
            Thread.sleep(1100L);
        } catch (final InterruptedException e) {
        }
    }


    @Timeout(1000L)
    public void timeoutWithIOException() throws IOException {
        throw new IOException();
    }

}
