package com.airepublic.microprofile.core;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import com.airepublic.microprofile.core.spi.ICDIServiceProvider;
import com.airepublic.microprofile.feature.logging.java.SerializableLogger;

public class Bootstrap {
    private static final Logger LOG = new SerializableLogger(Bootstrap.class.getName());


    private void startServer(final SeContainer cdiContainer) throws IOException {
        final JavaServer javaServer = cdiContainer.select(JavaServer.class).get();
        LOG.info("Booting microprofile-server ...");
        javaServer.start(cdiContainer);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                javaServer.stop();
            }
        });

    }


    public static void start() throws IOException {
        try {

            final ServiceLoader<ICDIServiceProvider> providers = ServiceLoader.load(ICDIServiceProvider.class);
            SeContainer cdiContainer;

            if (providers.findFirst().isPresent()) {
                cdiContainer = providers.findFirst().get().getSeContainer();
            } else {
                cdiContainer = SeContainerInitializer.newInstance().initialize();
            }

            new Bootstrap().startServer(cdiContainer);
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Failed to start server: ", e);
        }
    }
}
