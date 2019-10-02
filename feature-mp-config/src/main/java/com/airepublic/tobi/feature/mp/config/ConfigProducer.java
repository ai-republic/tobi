package com.airepublic.tobi.feature.mp.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * CDI producer to provide a {@link Config}.
 * 
 * @author Torsten Oltmanns
 *
 */
@ApplicationScoped
public class ConfigProducer {
    private Config config;


    /**
     * Produces the {@link Config}.
     * 
     * @return the {@link Config}
     */
    @Produces
    public Config produceConfig() {
        if (config == null) {
            config = ConfigProviderResolver.instance().getConfig();
        }

        return config;
    }

}
