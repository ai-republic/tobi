package com.airepublic.microprofile.core;

import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigProducer implements IConfigConstants {
    private static final ServerContext serverContext;
    private static final Config config;

    static {
        config = ConfigProvider.getConfig();
        serverContext = ServerContext.create(config);
    }


    @Produces
    protected ServerContext getServerContext() {
        return serverContext;
    }


    @Produces
    public Config getConfig() {
        return config;
    }

}
