package com.airepublic.tobi.feature.mp.config;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * Microprofile {@link ConfigBuilder} implementation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ConfigBuilderImpl implements ConfigBuilder {
    private final ConfigImpl config = ConfigImpl.create();
    private final List<ConfigSource> discoveredSources;
    private final List<Converter<?>> discoveredConverters;


    /**
     * Constructor.
     */
    @SuppressWarnings("rawtypes")
    public ConfigBuilderImpl() {
        config.setClassLoader(Thread.currentThread().getContextClassLoader());
        final ServiceLoader<ConfigSource> slConfigSource = ServiceLoader.load(ConfigSource.class);
        discoveredSources = slConfigSource.stream().map(sl -> sl.get()).collect(Collectors.toList());

        final ServiceLoader<Converter> slConverter = ServiceLoader.load(Converter.class);
        discoveredConverters = slConverter.stream().map(sl -> (Converter<?>) sl.get()).collect(Collectors.toList());
    }


    @Override
    public ConfigBuilder addDefaultSources() {
        config.addConfigSource(new SystemPropertiesConfigSource());
        config.addConfigSource(new EnvironmentConfigSource());
        config.addConfigSource(new MicroprofileDefaultConfigSource());
        return this;
    }


    @Override
    public ConfigBuilder addDiscoveredSources() {
        for (final ConfigSource configSource : discoveredSources) {
            config.addConfigSource(configSource);
        }

        return this;
    }


    @Override
    public ConfigBuilder addDiscoveredConverters() {
        for (final Converter<?> converter : discoveredConverters) {
            config.addConverter(converter, 100);
        }

        return this;
    }


    @Override
    public ConfigBuilder forClassLoader(final ClassLoader loader) {
        config.setClassLoader(loader);
        return this;
    }


    @Override
    public ConfigBuilder withSources(final ConfigSource... sources) {
        for (final ConfigSource source : sources) {
            config.addConfigSource(source);
        }

        return this;
    }


    @Override
    public ConfigBuilder withConverters(final Converter<?>... converters) {
        for (final Converter<?> converter : converters) {
            config.addConverter(converter, 100);
        }

        return this;
    }


    @Override
    public <T> ConfigBuilder withConverter(final Class<T> type, final int priority, final Converter<T> converter) {
        Objects.requireNonNull(converter, "Converter must not be null!");
        config.addConverter(converter, priority);
        return this;
    }


    @Override
    public Config build() {
        return config;
    }
}
