package com.airepublic.microprofile.testsuite.faulttolerance;

import java.io.IOException;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WeldSetup
public class CircuitBreakerTests {
    private CircuitBreakerTestClass test;


    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(CircuitBreakerTestClass.class).get();
    }


    @Test
    public void testCircuitBreakerFailOn() {
        for (int i = 0; i < 3; i++) {
            try {
                test.circuitBreakerFailOn();
            } catch (final Throwable e) {
            }
        }

        Assertions.assertEquals(2, test.counter);
    }


    @Test
    public void testCircuitBreakerSuccessThreshold() {
        // open the circuit by failing 2 times, the 3rd should be ignored
        for (int i = 0; i < 3; i++) {
            try {
                test.circuitBreakerSuccessThreshold(new IOException());
            } catch (final Throwable e) {
            }
        }

        Assertions.assertEquals(2, test.counter);

        // wait for the delay to pass
        try {
            Thread.sleep(1000L);
        } catch (final InterruptedException e1) {
        }

        // exceed the successThreshold by causing a not-failed on exception
        for (int i = 0; i < 2; i++) {
            try {
                test.circuitBreakerSuccessThreshold(new RuntimeException());
            } catch (final Throwable e) {
            }
        }

        Assertions.assertEquals(4, test.counter);
    }


    @Test
    public void testCircuitBreakerRefailingSuccessThreshold() {
        // open the circuit by failing 2 times, the 3rd should be ignored
        for (int i = 0; i < 3; i++) {
            try {
                test.circuitBreakerSuccessThreshold(new IOException());
            } catch (final Throwable e) {
            }
        }

        Assertions.assertEquals(2, test.counter);

        // wait for the delay to pass
        try {
            Thread.sleep(1000L);
        } catch (final InterruptedException e1) {
        }

        // increment the successCount by causing a not-failed on exception
        try {
            test.circuitBreakerSuccessThreshold(new RuntimeException());
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(3, test.counter);

        // fail again
        try {
            test.circuitBreakerSuccessThreshold(new IOException());
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(4, test.counter);

        // should still be closed
        try {
            test.circuitBreakerSuccessThreshold(new RuntimeException());
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(4, test.counter);

        // wait for the delay to pass
        try {
            Thread.sleep(1000L);
        } catch (final InterruptedException e1) {
        }

        // should need 2 more successful calls to close
        for (int i = 0; i < 2; i++) {
            try {
                test.circuitBreakerSuccessThreshold(new RuntimeException());
            } catch (final Throwable e) {
            }
        }

        Assertions.assertEquals(6, test.counter);

        // should need 2 times to open the circuit again by failing , the 3rd should be ignored
        for (int i = 0; i < 3; i++) {
            try {
                test.circuitBreakerSuccessThreshold(new IOException());
            } catch (final Throwable e) {
            }
        }

        Assertions.assertEquals(8, test.counter);
    }

}
