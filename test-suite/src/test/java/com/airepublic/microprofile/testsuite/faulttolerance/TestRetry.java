package com.airepublic.microprofile.testsuite.faulttolerance;

import java.io.IOException;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WeldSetup
public class TestRetry {
    private RetryCombinations test;


    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(RetryCombinations.class).get();
    }


    @Test
    public void testRetryAbortOnIOException() {
        try {
            test.retryAbortOn(new IOException());
            Assertions.fail("Should abort on IOException");
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(1, test.counter);
    }


    @Test
    public void testRetryAbortOnOtherException() {
        try {
            test.retryAbortOn(new RuntimeException());
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(4, test.counter);
    }


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
            Assertions.assertTrue(nextTime - time > 1000);
            time = nextTime;
        }
    }


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
            Assertions.assertTrue(nextTime - time > 1000);
            time = nextTime;
        }
    }


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
                Assertions.assertTrue(nextTime - time > 900 && nextTime - time < 1200);
                time = nextTime;
            }
        }
    }


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


    @Test
    public void testRetryMaxDuration() {
        try {
            test.retryMaxDuration();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(3, test.counter);
    }


    @Test
    public void testRetryMaxDurationWithUnit() {
        try {
            test.retryMaxDurationWithUnit();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(3, test.counter);
    }


    @Test
    public void testRetryMaxRetries() {
        try {
            test.retryMaxRetries();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(3, test.counter);
    }

}
