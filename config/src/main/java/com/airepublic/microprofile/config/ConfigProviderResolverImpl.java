package com.airepublic.microprofile.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

public class ConfigProviderResolverImpl extends ConfigProviderResolver {
    private final static Map<ClassLoader, Config> CONFIGS = new ConcurrentHashMap<>();
    private final Config defaultConfig = new ConfigImpl();


    @Override
    public Config getConfig() {
        return CONFIGS.getOrDefault(Thread.currentThread().getContextClassLoader(), defaultConfig);
    }


    @Override
    public Config getConfig(final ClassLoader loader) {
        return CONFIGS.getOrDefault(loader, defaultConfig);
    }


    @Override
    public ConfigBuilder getBuilder() {
        return new ConfigBuilderImpl();
    }


    @Override
    public void registerConfig(final Config config, final ClassLoader classLoader) {
        ClassLoader cl = classLoader;

        if (classLoader == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }

        if (config instanceof ConfigImpl) {
            ((ConfigImpl) config).setClassLoader(cl);
        }

        CONFIGS.put(cl, config);
    }


    @Override
    public void releaseConfig(final Config config) {
        if (config instanceof ConfigImpl) {
            CONFIGS.remove(((ConfigImpl) config).getClassLoader());
        } else {
            ClassLoader cl = null;

            for (final Map.Entry<ClassLoader, Config> entry : CONFIGS.entrySet()) {
                cl = entry.getKey();
            }

            if (cl != null) {
                CONFIGS.remove(cl);
            }
        }
    }
}
