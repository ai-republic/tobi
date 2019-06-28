package com.airepublic.microprofile.feature.mp.config;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.airepublic.microprofile.feature.logging.java.SerializableLogger;

/**
 * Implementation of the default {@link ConfigSource} to provide properties stored in a resource
 * under /META-INF/microprofile-config.properties as config properties.
 * 
 * @author Torsten Oltmanns
 *
 */
public class MicroprofileDefaultConfigSource implements ConfigSource {
    private static final Logger LOG = new SerializableLogger(MicroprofileDefaultConfigSource.class.getName());
    private final Map<String, String> properties = new HashMap<>();


    public MicroprofileDefaultConfigSource() {
        try {
            final URL url = getClass().getClassLoader().getResource("META-INF/microprofile-config.properties");
            final Path file = Paths.get(url.toURI());
            final List<String> lines = Files.readAllLines(file);
            lines.forEach(line -> {
                if (line.indexOf("=") != -1) {
                    final StringTokenizer tokenizer = new StringTokenizer(line);
                    final String key = tokenizer.nextToken("=").strip();

                    String value = tokenizer.nextToken("").strip();

                    if (value.length() > 0) {
                        value = value.substring(1, value.length());
                    }

                    properties.put(key, value);
                }
            });
        } catch (final Exception e) {
            LOG.warning("Config under /META-INF/microprofile-config.properties could not be read!");
        }
    }


    @Override
    public int getOrdinal() {
        return 100;
    }


    @Override
    public String getValue(final String propertyName) {
        return properties.get(propertyName);
    }


    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }


    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
