package com.airepublic.microprofile.testsuite.faulttolerance;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FallbackTests {
    private FallbackTestClass test;


    @BeforeEach
    public void setUp() {
        test = SeContainerInitializer.newInstance().initialize().select(FallbackTestClass.class).get();
    }


    @Test
    public void testFallbackMethod() {
        try {
            test.fallbackFallbackMethod();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(1, test.methodCounter);
    }


    @Test
    public void testFallbackHandler() {
        try {
            test.fallbackFallbackHandler();
        } catch (final Throwable e) {
        }

        Assertions.assertEquals(1, test.handlerCounter);
    }
}
