package com.airepublic.tobi.feature.mp.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Implementation of the default {@link ConfigSource} to provide system properties as config
 * properties.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SystemPropertiesConfigSource implements ConfigSource {

    @Override
    public int getOrdinal() {
        return 400;
    }


    @Override
    public String getValue(final String propertyName) {
        return System.getProperty(propertyName);
    }


    @Override
    public Map<String, String> getProperties() {
        // read the properties everytime fresh in case of changes
        final Map<String, String> properties = new HashMap<>();
        System.getProperties().entrySet().forEach(e -> properties.put(e.getKey().toString(), e.getValue().toString()));
        return Collections.unmodifiableMap(properties);
    }


    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
