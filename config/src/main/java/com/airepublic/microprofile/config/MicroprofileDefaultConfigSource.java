package com.airepublic.microprofile.config;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the default {@link ConfigSource} to provide properties stored in a resource
 * under META-INF/microprofile-config.properties as config properties.
 * 
 * @author Torsten Oltmanns
 *
 */
public class MicroprofileDefaultConfigSource implements ConfigSource {
    private static final Logger LOG = LoggerFactory.getLogger(MicroprofileDefaultConfigSource.class);
    private final Map<String, String> properties = new HashMap<>();


    public MicroprofileDefaultConfigSource() {
        try {
            final URL url = getClass().getClassLoader().getResource("META-INF/microprofile-config.properties");
            final Path file = Paths.get(url.toURI());
            final List<String> lines = Files.readAllLines(file);
            lines.forEach(line -> {
                final StringTokenizer tokenizer = new StringTokenizer(line);
                final String key = tokenizer.nextToken("=").strip();
                String value = tokenizer.nextToken("").strip();

                if (value.length() > 0) {
                    value = value.substring(1, value.length());
                }

                properties.put(key, value);
            });
        } catch (final Exception e) {
            LOG.warn("Config under META-INF/microprofile-config.properties could not be read!");
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


    public static void main(final String[] args) {
        System.out.println(new MicroprofileDefaultConfigSource().getProperties());
    }
}
