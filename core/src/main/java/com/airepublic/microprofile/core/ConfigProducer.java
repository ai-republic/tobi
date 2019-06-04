package com.airepublic.microprofile.core;

import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigProducer implements IConfigConstants {
    private final static String DEFAULT_HOST = "localhost";
    private final static int DEFAULT_PORT = 8080;
    private final static int DEFAULT_SSL_PORT = 8443;
    private final static int DEFAULT_WORKER_COUNT = 10;
    private static final ServerContext serverContext;
    private static final Config config;

    static {

        config = ConfigProvider.getConfig();
        final String host = config.getOptionalValue(HOST, String.class).orElse(DEFAULT_HOST);
        final int port = config.getOptionalValue(PORT, Integer.class).orElse(DEFAULT_PORT);
        final int sslPort = config.getOptionalValue(SSL_PORT, Integer.class).orElse(DEFAULT_SSL_PORT);
        final int workerCount = config.getOptionalValue(WORKER_COUNT, Integer.class).orElse(DEFAULT_WORKER_COUNT);
        final String keystorePassword = config.getOptionalValue(KEYSTORE_PASSWORD, String.class).orElse("changeit");
        final String truststorePassword = config.getOptionalValue(TRUSTSTORE_PASSWORD, String.class).orElse("changeit");

        serverContext = ServerContext.create(host, port, sslPort)
                .setWorkerCount(workerCount)
                .setKeystorePassword(keystorePassword)
                .setTruststorePassword(truststorePassword);
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
