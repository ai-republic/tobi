package com.airepublic.tobi.feature.mp.config;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI producer for {@link ConfigProperty}s.
 * 
 * @author Torsten Oltmanns
 *
 */
@ApplicationScoped
public class ConfigPropertyProducer {
    @Inject
    private Config config;


    /**
     * Produces the value for the {@link ConfigProperty} for the injection point.
     * 
     * @param <T> the type
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @SuppressWarnings("unchecked")
    public <T> T produceConfigProperty(final InjectionPoint ip) {
        // check if the InjectionPoint refers to a field
        if (ip.getMember() instanceof Field) {
            final Field field = (Field) ip.getMember();
            final ConfigProperty cp = ip.getAnnotated().getAnnotations(ConfigProperty.class).stream().findFirst().get();
            final T t = (T) config.getValue(cp.name(), field.getType());

            // check if the field is of type Optional
            if (field.getType() == Optional.class) {
                if (t == null) {
                    final ParameterizedType pt = (ParameterizedType) field.getGenericType();
                    final String defaultValue = cp.defaultValue();

                    if (defaultValue == null || defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                        return null;
                    }

                    return (T) Optional.of(((ConfigImpl) config).convert(defaultValue, (Class<?>) pt.getActualTypeArguments()[0]));
                }

                return (T) Optional.of(t);
                // check if the field is of type Provider
            } else if (field.getType() == Provider.class) {
                final Provider<T> provider = () -> t;
                return (T) provider;
            } else {
                // check if the default value should be injected
                if (t == null) {
                    final String defaultValue = cp.defaultValue();

                    if (defaultValue == null || defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                        return null;
                    }

                    return (T) ((ConfigImpl) config).convert(defaultValue, field.getType());
                }

                return t;
            }
        }

        return null;
    }


    /**
     * Produces a {@link String} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    public String produceConfigPropertyString(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    /**
     * Produces an {@link Integer} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    public Integer produceConfigPropertyInteger(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    /**
     * Produces a {@link Float} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    public Float produceConfigPropertyFloat(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    /**
     * Produces a {@link Double} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    public Double produceConfigPropertyDouble(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    /**
     * Produces a {@link Optional} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    @SuppressWarnings("rawtypes")
    public Optional produceConfigPropertyOptional(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    /**
     * Produces a {@link Provider} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    @SuppressWarnings("rawtypes")
    public Provider produceConfigPropertyProvider(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    /**
     * Produces a {@link Class} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    @SuppressWarnings("rawtypes")
    public Class produceConfigPropertyClass(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    /**
     * Produces a {@link Collection} value for the {@link ConfigProperty} for the injection point.
     * 
     * @param ip the {@link InjectionPoint}
     * @return the resolved value
     */
    @Produces
    @ConfigProperty
    @SuppressWarnings("rawtypes")
    public Collection produceConfigPropertyCollection(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }
}
