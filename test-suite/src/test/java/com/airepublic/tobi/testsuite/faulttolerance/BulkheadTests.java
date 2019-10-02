package com.airepublic.tobi.testsuite.faulttolerance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Bulkhead} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
@WeldSetup
public class BulkheadTests {
    private BulkheadTestClass test;


    /**
     * Setup.
     */
    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(BulkheadTestClass.class).get();
    }


    /**
     * Test simple use of {@link Bulkhead} annotation.
     * 
     * @throws Exception if something fails
     */
    @Test
    public void testBulkheadWithoutAsynchronous() throws Exception {
        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            final String result = test.bulkheadWithoutAsynchronous();
            Assertions.assertEquals("Test", result);
        }

        Assertions.assertEquals(3, test.counter);
        final long duration = System.currentTimeMillis() - startTime;
        Assertions.assertTrue(duration > 3000L && duration < 3500L);
    }


    /**
     * Test use of {@link Bulkhead} annotation in combination with {@link Asynchronous}.
     * 
     * @throws Exception if something fails
     */
    @Test
    public void testBulkheadWithAsynchronous() throws InterruptedException, ExecutionException {
        final long startTime = System.currentTimeMillis();
        final List<Future<String>> results = new ArrayList<>();

        new Thread(() -> {
            System.out.println("started");
        }).start();

        // start 3 parallel tasks
        for (int i = 0; i < 3; i++) {
            final Future<String> result = test.bulkheadWithAsynchronous();
            results.add(result);
        }

        // 2 should be run in parallel 1 afterwards
        for (final Future<String> result : results) {
            Assertions.assertEquals("Test", result.get());
        }

        // the execution time should at least be 2000ms (1 parallel + 1 afterwards)
        final long duration = System.currentTimeMillis() - startTime;
        Assertions.assertTrue(duration > 2000L && duration < 3000L);

        Assertions.assertEquals(3, test.counter);
    }
}
