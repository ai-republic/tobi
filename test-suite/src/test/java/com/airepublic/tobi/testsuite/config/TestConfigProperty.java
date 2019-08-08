package com.airepublic.tobi.testsuite.config;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

public class TestConfigProperty {
    @Inject
    @ConfigProperty(name = "user.home")
    private String test;


    @Test
    public void testConfigPropertyInjection() {
        final SeContainer container = SeContainerInitializer.newInstance().initialize();
        final TestConfigProperty test = container.select(TestConfigProperty.class).get();
        System.out.println(test.test);
    }
}
