package com.airepublic.microprofile.feature.logging.java;

import java.util.logging.Logger;

import javax.inject.Inject;

import com.airepublic.microprofile.feature.logging.java.LoggerConfig.Resource;

public class LoggerProducerTestClass {
    @Inject
    private Logger defaultLogger;
    @Inject
    @LoggerConfig(name = "L1", level = LogLevel.INFO)
    private Logger configuredLogger;
    @Inject
    @LoggerConfig(name = "L2", level = LogLevel.WARNING, resource = @Resource(baseName = "logger-resource"))
    private Logger configuredLoggerWithResources;


    public Logger getDefaultLogger() {
        return defaultLogger;
    }


    public Logger getConfiguredLogger() {
        return configuredLogger;
    }


    public Logger getConfiguredLoggerWithResources() {
        return configuredLoggerWithResources;
    }

}
