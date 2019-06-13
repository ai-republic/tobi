package com.airepublic.microprofile.core;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class ServerContextProducer {
    private static ServerContext serverContext;


    @Produces
    protected ServerContext getServerContext(final Config config) {
        if (serverContext == null) {
            serverContext = ServerContext.create(config);
        }

        return serverContext;
    }
}
