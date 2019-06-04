package com.airepublic.microprofile.config;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

public class ConfigBuilderImpl implements ConfigBuilder {
    private final ConfigImpl config = new ConfigImpl();
    private final List<ConfigSource> discoveredSources;
    private final List<Converter<?>> discoveredConverters;


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
            config.addConverter(converter);
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
            config.addConverter(converter);
        }

        return this;
    }


    @Override
    public <T> ConfigBuilder withConverter(final Class<T> type, final int priority, final Converter<T> converter) {
        // TODO add priority and special class handling in ConfigImpl
        config.addConverter(converter);
        return this;
    }


    @Override
    public Config build() {
        return config;
    }
}
