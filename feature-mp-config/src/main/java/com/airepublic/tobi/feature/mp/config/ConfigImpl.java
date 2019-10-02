package com.airepublic.tobi.feature.mp.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.airepublic.logging.java.SerializableLogger;

/**
 * Implementation of the Microprofile {@link Config} interface.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ConfigImpl implements Config {
    private final static Logger LOG = new SerializableLogger(ConfigImpl.class.getName());
    private final Map<String, String> values = new ConcurrentHashMap<>();
    private final Queue<ConfigSource> configSources = new ConcurrentLinkedQueue<>();
    private final Map<Class<?>, PriorizedConverter> converters = new HashMap<>();
    private ClassLoader classLoader;


    /**
     * Create a new {@link Config}.
     * 
     * @return the created {@link Config}
     */
    static ConfigImpl create() {
        return new ConfigImpl("");
    }


    /**
     * Dummy constructor to prevent CDI finding multiple instances. Should be using create() or the
     * {@link ConfigPropertyProducer}.
     * 
     * @param dummy not used
     */
    private ConfigImpl(final String dummy) {
    }


    @Override
    public <T> T getValue(final String propertyName, final Class<T> propertyType) {
        final String propertyValue = values.get(propertyName);

        return convert(propertyValue, propertyType);
    }


    /**
     * Convert the specified value to the specified type. If the propertyValue is an array, each
     * element in the array is converted.
     * 
     * @param <T> the type of object
     * @param propertyValue the value
     * @param propertyType the class of the return type
     * @return the converted value
     */
    @SuppressWarnings("unchecked")
    protected <T> T convert(final String propertyValue, final Class<T> propertyType) {
        if (propertyValue == null) {
            return null;
        }

        // check if type is an array
        if (propertyType.isArray()) {
            // then parse the values
            final List<String> propertyValues = parse(propertyValue);
            final List<T> convertedValues = new ArrayList<>();

            // and convert each element
            for (final String value : propertyValues) {
                final T convertedValue = convertSingleValue(value, propertyType);

                if (convertedValue != null) {
                    convertedValues.add(convertedValue);
                }
            }

            if (convertedValues.size() == 0) {
                return null;
            }

            return (T) convertedValues.toArray();
        } else {
            // otherwise convert the value
            return convertSingleValue(propertyValue, propertyType);
        }
    }


    /**
     * Parses the speciied value to a list of values.
     * 
     * @param propertyValue the value
     * @return the list of values
     */
    protected List<String> parse(final String propertyValue) {
        final List<String> values = new ArrayList<>();
        final StringBuffer buf = new StringBuffer();

        for (int i = 0; i < propertyValue.length(); i++) {
            final char chr = propertyValue.charAt(i);

            if (chr == ',' && i > 0 && propertyValue.charAt(i - 1) != '\\') {
                values.add(buf.toString());
                buf.setLength(0);
            }
        }

        return values;
    }


    /**
     * Converts the value to the specified type.
     * 
     * @param <T> the object type
     * @param propertyValue the value
     * @param propertyType the class of the type
     * @return the converted value to the type
     */
    @SuppressWarnings("unchecked")
    protected <T> T convertSingleValue(final String propertyValue, final Class<?> propertyType) {
        T convertedValue = null;

        if (propertyType == String.class) {
            convertedValue = (T) propertyValue;
        }

        if (propertyType == Integer.class || propertyType == int.class) {
            convertedValue = (T) Integer.valueOf(propertyValue);
        }

        if (propertyType == Long.class || propertyType == long.class) {
            convertedValue = (T) Long.valueOf(propertyValue);
        }

        if (propertyType == Float.class || propertyType == float.class) {
            convertedValue = (T) Double.valueOf(propertyValue);
        }

        if (propertyType == Double.class || propertyType == double.class) {
            convertedValue = (T) Double.valueOf(propertyValue);
        }

        if (propertyType.getName().equals("java.lang.Class")) {
            try {
                convertedValue = (T) Class.forName(propertyValue);
            } catch (final ClassNotFoundException e) {
                LOG.severe("Configuration " + propertyValue + " doesn't represent a valid classname");
            }
        }

        final PriorizedConverter converter = converters.get(propertyType);

        if (converter != null) {
            convertedValue = (T) converter.getConverter().convert(propertyValue);
        }

        // check conversion methods of propertyType according to spec
        if (convertedValue == null) {
            try {
                // check for public static T of(String) method
                final Method method = propertyType.getMethod("of", String.class);

                if (method != null && (method.getModifiers() & Modifier.STATIC & Modifier.PUBLIC) == method.getModifiers()) {
                    convertedValue = (T) method.invoke(null, propertyValue);
                }
            } catch (final Exception e) {
            }

            try {
                // check for public static T valueOf(String) method
                final Method method = propertyType.getMethod("valueOf", String.class);

                if (method != null && (method.getModifiers() & Modifier.STATIC & Modifier.PUBLIC) == method.getModifiers()) {
                    convertedValue = (T) method.invoke(null, propertyValue);
                }
            } catch (final Exception e) {
            }

            try {
                // check for public T(String) constructor
                convertedValue = (T) propertyType.getConstructor(String.class).newInstance(propertyValue);
            } catch (final Exception e) {
            }

            try {
                // check for public static T parse(CharSequence) method
                final Method method = propertyType.getMethod("parse", CharSequence.class);

                if (method != null && (method.getModifiers() & Modifier.STATIC & Modifier.PUBLIC) == method.getModifiers()) {
                    convertedValue = (T) method.invoke(null, propertyValue);
                }
            } catch (final Exception e) {
            }

        }

        return convertedValue;
    }


    @Override
    public <T> Optional<T> getOptionalValue(final String propertyName, final Class<T> propertyType) {
        return Optional.ofNullable(getValue(propertyName, propertyType));
    }


    @Override
    public Iterable<String> getPropertyNames() {
        return values.keySet();
    }


    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources;
    }


    /**
     * Adds a {@link ConfigSource}.
     * 
     * @param configSource the {@link ConfigSource}
     */
    protected void addConfigSource(final ConfigSource configSource) {
        Objects.requireNonNull(configSource, "ConfigSource must not be null!");

        configSources.add(configSource);

        values.putAll(configSource.getProperties());
    }


    /**
     * Adds a {@link Converter} with the specified priority.
     * 
     * @param converter the {@link Converter}
     * @param priority the priority to order the converters
     */
    protected void addConverter(final Converter<?> converter, final int priority) {
        Objects.requireNonNull(converter, "Converter must not be null!");

        try {
            LOG.info("Adding converter: " + converter);
            final Class<?> clazz = converter.getClass().getMethod("convert", String.class).getReturnType();
            final PriorizedConverter currentConverter = converters.get(clazz);

            if (currentConverter != null) {
                if (currentConverter.getPriority() < priority) {
                    converters.put(clazz, new PriorizedConverter(converter, priority));
                }
            } else {
                converters.put(clazz, new PriorizedConverter(converter, priority));
            }

        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Converter " + converter + " could not be added!", e);
        }
    }


    /**
     * Gets the {@link ClassLoader} for the {@link Config}.
     * 
     * @return classLoader the {@link ClassLoader}
     */
    protected ClassLoader getClassLoader() {
        return classLoader;
    }


    /**
     * Sets the {@link ClassLoader} for the {@link Config}.
     * 
     * @param classLoader the {@link ClassLoader}
     */
    protected void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
