package com.airepublic.microprofile.testsuite.faulttolerance;

import java.io.IOException;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WeldSetup
public class TimeoutTests {
    private TimeoutTestClass test;


    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(TimeoutTestClass.class).get();
    }


    @Test
    public void testTimeout() {
        try {
            test.timeout();
            Assertions.fail();
        } catch (final Exception e) {
            Assertions.assertEquals(TimeoutException.class, e.getClass());
        }
    }


    @Test
    public void testTimeoutWithIOException() {
        try {
            test.timeoutWithIOException();
            Assertions.fail();
        } catch (final Exception e) {
            Assertions.assertEquals(IOException.class, e.getClass());
        }
    }

}
