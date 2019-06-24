package com.airepublic.microprofile.feature.mp.config;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConfigPropertyProducer {
    @Inject
    private Config config;


    @SuppressWarnings("unchecked")
    public <T> T produceConfigProperty(final InjectionPoint ip) {
        if (ip.getMember() instanceof Field) {
            final Field field = (Field) ip.getMember();
            final ConfigProperty cp = ip.getAnnotated().getAnnotations(ConfigProperty.class).stream().findFirst().get();
            final T t = (T) config.getValue(cp.name(), field.getType());

            if (field.getType() == Optional.class) {
                if (t == null) {
                    final ParameterizedType pt = (ParameterizedType) field.getGenericType();
                    return (T) Optional.of(((ConfigImpl) config).convert(cp.defaultValue(), (Class<?>) pt.getActualTypeArguments()[0]));
                }

                return (T) Optional.of(t);
            } else if (field.getType() == Provider.class) {
                final Provider<T> provider = () -> t;
                return (T) provider;
            } else {
                if (t == null) {
                    return (T) ((ConfigImpl) config).convert(cp.defaultValue(), field.getType());
                }

                return t;
            }
        }

        return null;
    }


    @Produces
    @ConfigProperty
    public String produceConfigPropertyString(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    @Produces
    @ConfigProperty
    public Integer produceConfigPropertyInteger(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    @Produces
    @ConfigProperty
    public Float produceConfigPropertyFloat(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }


    @Produces
    @ConfigProperty
    public Double produceConfigPropertyDouble(final InjectionPoint ip) {
        return produceConfigProperty(ip);
    }

}
