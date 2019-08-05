package com.airepublic.tobi.feature.mp.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

@ApplicationScoped
public class ConfigProducer {
    private Config config;


    @Produces
    public Config produceConfig() {
        if (config == null) {
            config = ConfigProviderResolver.instance().getConfig();
        }

        return config;
    }

}
