package com.airepublic.tobi.testsuite.config;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ConfigProperty} injection.
 * 
 * @author Torsten Oltmanns
 *
 */
public class TestConfigProperty {
    @Inject
    @ConfigProperty(name = "user.home")
    private String test;


    /**
     * Test the injection of a {@link ConfigProperty}.
     */
    @Test
    public void testConfigPropertyInjection() {
        final SeContainer container = SeContainerInitializer.newInstance().initialize();
        final TestConfigProperty test = container.select(TestConfigProperty.class).get();
        Assertions.assertNotNull(test.test);
    }
}
