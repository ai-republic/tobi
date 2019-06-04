package com.airepublic.microprofile.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigImpl implements Config {
    private final static Logger LOG = LoggerFactory.getLogger(ConfigImpl.class);
    private final Map<String, ?> values = new ConcurrentHashMap<>();
    private final Queue<ConfigSource> configSources = new ConcurrentLinkedQueue<>();
    private final Map<Class<?>, Converter<?>> converters = new HashMap<>();
    private ClassLoader classLoader;


    @Override
    public <T> T getValue(final String propertyName, final Class<T> propertyType) {
        return propertyType.cast(values.get(propertyName));
    }


    @Override
    public <T> Optional<T> getOptionalValue(final String propertyName, final Class<T> propertyType) {
        return Optional.ofNullable(propertyType.cast(values.get(propertyName)));
    }


    @Override
    public Iterable<String> getPropertyNames() {
        return values.keySet();
    }


    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources;
    }


    void addConfigSource(final ConfigSource configSource) {
        configSources.add(configSource);
    }


    public void addConverter(final Converter<?> converter) {
        try {
            LOG.debug("Adding converter: " + converter);
            final Class<?> clazz = converter.getClass().getMethod("convert", String.class).getReturnType();
            converters.put(clazz, converter);

        } catch (final NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    ClassLoader getClassLoader() {
        return classLoader;
    }

}
