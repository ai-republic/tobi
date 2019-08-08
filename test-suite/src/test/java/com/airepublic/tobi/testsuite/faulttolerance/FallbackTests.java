package com.airepublic.tobi.testsuite.faulttolerance;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class to test the use of the {@link Fallback} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class FallbackTests {
    private FallbackTestClass test;


    /**
     * Setup.
     */
    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(FallbackTestClass.class).get();
    }


    /**
     * Test the method forwarding {@link Fallback} annotation.
     */
    @Test
    public void testFallbackMethod() {
        try {
            test.fallbackFallbackMethod();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(1, test.methodCounter);
    }


    /**
     * Test the use of a {@link FallbackHandler} in the {@link Fallback} annotation.
     */
    @Test
    public void testFallbackHandler() {
        try {
            test.fallbackFallbackHandler();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(1, FallbackTestClass.handlerCounter);
    }
}
