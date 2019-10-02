package com.airepublic.tobi.testsuite.faulttolerance;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Asynchronous} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
@WeldSetup
public class AsynchronousTests {
    private AsynchronousTestClass test;


    /**
     * Setup.
     */
    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(AsynchronousTestClass.class).get();
    }


    /**
     * Test simple {@link Asynchronous} use.
     * 
     * @throws Exception if something fails
     */
    @Test
    public void testAsynchronous() throws Exception {
        final CompletableFuture<String> result = test.asynchronous();
        Assertions.assertEquals("Test", result.get());
        Assertions.assertEquals(1, test.counter);
    }


    /**
     * Test {@link Asynchronous} use with exception on the {@link Future}.
     * 
     * @throws Exception if something fails
     */
    @Test
    public void testAsynchronousWithExceptionOnFuture() {
        try {
            final Future<String> result = test.asynchronousWithExceptionOnFuture();
            result.get();
        } catch (final Exception e) {
            if (!IOException.class.isAssignableFrom(e.getCause().getClass())) {
                Assertions.fail();
            }
        }

        Assertions.assertEquals(1, test.counter);
    }


    /**
     * Test simple {@link Asynchronous} use with exception on the {@link CompletionStage}.
     * 
     * @throws Exception if something fails
     */
    @Test
    public void testAsynchronousWithExceptionOnCompletionStage() {
        final CompletionStage<String> result = test.asynchronousWithExceptionOnCompletionStage();
        result.exceptionally(e -> {
            if (!IOException.class.isAssignableFrom(e.getClass())) {
                Assertions.fail();
            }
            return null;
        });

        Assertions.assertEquals(1, test.counter);
    }


    /**
     * Test {@link Asynchronous} on class level.
     * 
     * @throws Exception if something fails
     */
    @Test
    public void testAsynchronousOnClass() throws Exception {
        final CompletableFuture<String> result = test.asynchronousOnClass();
        Assertions.assertEquals("Test", result.get());
        Assertions.assertEquals(1, test.counter);
    }

}
