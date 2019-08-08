package com.airepublic.tobi.testsuite.faulttolerance;

import java.io.IOException;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Retry} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
@WeldSetup
public class RetryTests {
    private RetryTestClass test;


    /**
     * Setup.
     */
    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(RetryTestClass.class).get();
    }


    /**
     * Test the {@link Retry} annotation abortOn parameter.
     */
    @Test
    public void testRetryAbortOnIOException() {
        try {
            test.retryAbortOn(new IOException());
            Assertions.fail("Should abort on IOException");
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(1, test.counter);
    }


    /**
     * Test the {@link Retry} annotation abortOn parameter with non-watched exception.
     */
    @Test
    public void testRetryAbortOnOtherException() {
        try {
            test.retryAbortOn(new RuntimeException());
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(4, test.counter);
    }


    /**
     * Test the {@link Retry} annotation delay parameter.
     */
    @Test
    public void testRetryDelay() {
        try {
            test.retryDelay();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(4, test.counter);

        Long time = test.callTimes.get(0);

        for (int i = 1; i < test.callTimes.size(); i++) {
            final Long nextTime = test.callTimes.get(i);
            final long duration = nextTime - time;
            Assertions.assertTrue(duration >= 1000);
            time = nextTime;
        }
    }


    /**
     * Test the {@link Retry} annotation delay with delayUnit parameter.
     */
    @Test
    public void testRetryDelayWithUnit() {
        try {
            test.retryDelayWithUnit();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(4, test.counter);

        Long time = test.callTimes.get(0);

        for (int i = 1; i < test.callTimes.size(); i++) {
            final Long nextTime = test.callTimes.get(i);
            final long duration = nextTime - time;
            Assertions.assertTrue(duration >= 1000);
            time = nextTime;
        }
    }


    /**
     * Test the {@link Retry} annotation delay with jitter parameter.
     */
    @Test
    public void testRetryDelayWithJitter() {
        for (int x = 0; x < 3; x++) {

            try {
                test.callTimes.clear();
                test.counter = 0;
                test.retryDelayWithJitter();
            } catch (final Throwable e) {
            }

            Assertions.assertEquals(4, test.counter);

            Long time = test.callTimes.get(0);

            for (int i = 1; i < test.callTimes.size(); i++) {
                final Long nextTime = test.callTimes.get(i);
                final long duration = nextTime - time;
                Assertions.assertTrue(duration >= 900 && duration < 1200);
                time = nextTime;
            }
        }
    }


    /**
     * Test the {@link Retry} annotation delay with jitter and jitterDelayUnit parameter.
     */
    @Test
    public void testRetryDelayWithJitterWithUnit() {
        for (int x = 0; x < 3; x++) {

            try {
                test.callTimes.clear();
                test.counter = 0;
                test.retryDelayWithJitter();
            } catch (final Throwable e) {
            }

            Assertions.assertEquals(4, test.counter);

            Long time = test.callTimes.get(0);

            for (int i = 1; i < test.callTimes.size(); i++) {
                final Long nextTime = test.callTimes.get(i);
                Assertions.assertTrue(nextTime - time > 0 && nextTime - time < 2100);
                time = nextTime;
            }

        }
    }


    /**
     * Test the {@link Retry} annotation delay and maxDuration parameter.
     */
    @Test
    public void testRetryMaxDuration() {
        try {
            test.retryMaxDuration();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(3, test.counter);
    }


    /**
     * Test the {@link Retry} annotation delay and maxDuration with durationUnit parameter.
     */
    @Test
    public void testRetryMaxDurationWithUnit() {
        try {
            test.retryMaxDurationWithUnit();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(3, test.counter);
    }


    /**
     * Test the {@link Retry} annotation maxRetries parameter.
     */
    @Test
    public void testRetryMaxRetries() {
        try {
            test.retryMaxRetries();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(3, test.counter);
    }

}
