package com.airepublic.tobi.core;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import com.airepublic.tobi.core.spi.ICDIServiceProvider;

/**
 * Bootstrap for the Tobi server.
 * 
 * @author Torsten Oltmanns
 *
 */
public class Tobi {
    private static final Logger LOG = Logger.getGlobal();


    /**
     * Starts the Tobi server with the specified {@link SeContainer}.
     * 
     * @param cdiContainer the {@link SeContainer}
     * @param runAfterStart a task to run after the server has started
     * @throws IOException if something fails
     */
    private void startServer(final SeContainer cdiContainer, final Runnable runAfterStart) throws IOException {
        final TobiServer server = cdiContainer.select(TobiServer.class).get();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOG.info("Shutting down Tobi-server ...");
                server.stop();
            }
        });

        LOG.info("Booting Tobi-server ...");
        server.start(runAfterStart);
    }


    /**
     * Starts the Tobi server.
     * 
     * @throws IOException if something fails
     */
    public static void start() throws IOException {
        start(null);
    }


    /**
     * Starts the Tobi server.
     * 
     * @param runAfterStart a task to run after the server has started
     * @throws IOException if something fails
     */
    public static void start(final Runnable runAfterStart) throws IOException {
        try {

            final ServiceLoader<ICDIServiceProvider> providers = ServiceLoader.load(ICDIServiceProvider.class);
            SeContainer cdiContainer;

            if (providers.findFirst().isPresent()) {
                cdiContainer = providers.findFirst().get().getSeContainer();
            } else {
                cdiContainer = SeContainerInitializer.newInstance().initialize();
            }

            new Tobi().startServer(cdiContainer, runAfterStart);
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Failed to start Tobi server: ", e);
        }
    }

}
