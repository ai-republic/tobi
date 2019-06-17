package com.airepublic.microprofile.core;

import java.io.IOException;

import javax.enterprise.inject.se.SeContainer;
import javax.inject.Inject;

import org.jboss.weld.environment.se.Weld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    @Inject
    private JavaServer javaServer;
    @Inject
    private ServerContext serverContext;


    private void startServer(final SeContainer cdiContainer) throws IOException {
        serverContext.setCdiContainer(cdiContainer);
        javaServer.init();
        javaServer.run();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                javaServer.stop();
            }
        });

    }


    public static void start() throws IOException {
        try {

            // WebBeansFinder.setSingletonService(new JavaServerDefaultSingletonService());
            // cdiContainer = SeContainerInitializer.newInstance().setClassLoader(new
            // ClassLoader("bootstrap", LOG.getClass().getClassLoader()) {
            // }).initialize();
            // final Bootstrap bootstrap = cdiContainer.select(Bootstrap.class).get();
            final SeContainer cdiContainer = new Weld("javaserver").initialize();
            final Bootstrap bootstrap = cdiContainer.select(Bootstrap.class).get();

            LOG.info("Booting microprofile-server ...");
            bootstrap.startServer(cdiContainer);
        } catch (final Exception e) {
            LOG.error("Failed to start server: ", e);
        }
    }
}
