package com.airepublic.microprofile.config;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class Test {
    @Inject
    @ConfigProperty(name = "user.home")
    private String test;


    public static void main(final String[] args) {
        final SeContainer container = SeContainerInitializer.newInstance().initialize();
        final Test test = container.select(Test.class).get();
        System.out.println(test.test);
    }
}
