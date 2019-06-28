package com.airepublic.microprofile.feature.logging.java;

import java.util.logging.Level;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LoggerProducerTest {
    private static LoggerProducerTestClass testClass;


    @BeforeAll
    public static void setup() {
        testClass = SeContainerInitializer.newInstance().initialize().select(LoggerProducerTestClass.class).get();
        Assertions.assertNotNull(testClass);
    }


    @Test
    public void testProduceDefaultLogger() {
        Assertions.assertNotNull(testClass.getDefaultLogger());
        Assertions.assertEquals(Level.ALL, testClass.getDefaultLogger().getLevel());
    }


    @Test
    public void testProduceConfiguredLogger() {
        Assertions.assertNotNull(testClass.getConfiguredLogger());
        Assertions.assertEquals("L1", testClass.getConfiguredLogger().getName());
        Assertions.assertEquals(Level.INFO, testClass.getConfiguredLogger().getLevel());
    }


    @Test
    public void testProduceConfiguredLoggerWithResources() {
        Assertions.assertNotNull(testClass.getConfiguredLoggerWithResources());
        Assertions.assertEquals("L2", testClass.getConfiguredLoggerWithResources().getName());
        Assertions.assertNotNull(testClass.getConfiguredLoggerWithResources().getResourceBundle());
        Assertions.assertEquals(Level.WARNING, testClass.getConfiguredLoggerWithResources().getLevel());
    }

}
