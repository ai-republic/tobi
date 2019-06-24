package com.airepublic.microprofile.feature.mp.config;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Implementation of the default {@link ConfigSource} to provide environment properties as config
 * properties.
 * 
 * @author Torsten Oltmanns
 *
 */
public class EnvironmentConfigSource implements ConfigSource {

    @Override
    public int getOrdinal() {
        return 300;
    }


    @Override
    public String getValue(final String propertyName) {
        return System.getenv(propertyName);
    }


    @Override
    public Map<String, String> getProperties() {
        // read the environment everytime fresh in case of changes
        return Collections.unmodifiableMap(System.getenv());
    }


    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

}
