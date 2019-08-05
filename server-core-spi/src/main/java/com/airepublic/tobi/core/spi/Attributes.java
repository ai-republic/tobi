package com.airepublic.tobi.core.spi;

import java.util.HashMap;
import java.util.Map;

public class Attributes {
    private final Map<String, Object> attributes = new HashMap<>();


    public Attributes() {
    }


    public Attributes(final Attributes copy) {
        attributes.putAll(copy.attributes);
    }


    public void set(final String key, final Object value) {
        attributes.put(key, value);
    }


    public Object get(final String key) {
        return attributes.get(key);
    }


    public String getString(final String key) {
        final Object value = get(key);

        if (value == null) {
            return null;
        }

        return String.class.cast(value.toString());
    }


    public Integer getInt(final String key) {
        final Object value = get(key);

        if (value == null) {
            return null;
        }

        if (String.class.isAssignableFrom(value.getClass())) {
            return Integer.valueOf(value.toString());
        }

        return Integer.class.cast(value);
    }


    public Boolean getBoolean(final String key) {
        final Object value = get(key);

        if (value == null) {
            return null;
        }

        if (String.class.isAssignableFrom(value.getClass())) {
            return Boolean.valueOf(value.toString());
        }

        return Boolean.class.cast(value);
    }


    public <T> T get(final String key, final Class<T> type) {
        final Object value = get(key);

        if (value == null) {
            return null;
        }

        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }

        throw new ClassCastException("Attribute value " + value + " for key " + key + " cannot be cast to " + type.getName() + " because its of type " + value.getClass().getName());
    }

}
