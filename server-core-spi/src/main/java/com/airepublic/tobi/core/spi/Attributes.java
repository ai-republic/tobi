package com.airepublic.tobi.core.spi;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flexible attribute storage which can contain any type of objects registered under a string
 * identifier.
 * 
 * @author Torsten Oltmanns
 *
 */
public class Attributes {
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();


    /**
     * Constructor.
     */
    public Attributes() {
    }


    /**
     * Constructor.
     * 
     * @param copy {@link Attributes} which will be copied into this object
     */
    public Attributes(final Attributes copy) {
        attributes.putAll(copy.attributes);
    }


    /**
     * Returns a set of attribute keys.
     * 
     * @return the key set
     */
    public Set<String> keySet() {
        return attributes.keySet();
    }


    /**
     * Returns a set of attribute entries.
     * 
     * @return the entry set
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        return attributes.entrySet();
    }


    /**
     * Adds all entries of the map.
     * 
     * @param attributes the attributes to add
     */
    public void addAll(final Map<String, Object> attributes) {
        attributes.putAll(attributes);
    }


    /**
     * Checks whether the key exists.
     * 
     * @param key the key
     * @return true if the key exists, otherwise false
     */
    public boolean hasAttribute(final String key) {
        return attributes.containsKey(key);
    }


    /**
     * Sets a key/value pair.
     * 
     * @param key the key
     * @param value the value
     */
    public void setAttribute(final String key, final Object value) {
        if (value != null) {
            attributes.put(key, value);
        } else {
            attributes.remove(key);
        }
    }


    /**
     * Gets the value for the specified key or null if the key or value does not exist
     * 
     * @param key the key
     * @return the value or null
     */
    public Object getAttribute(final String key) {
        return attributes.get(key);
    }


    /**
     * Gets the string value for the specified key.
     * 
     * @param key the key
     * @return the value or null if it does not exist
     */
    public String getString(final String key) {
        final Object value = getAttribute(key);

        if (value == null) {
            return null;
        }

        return String.class.cast(value.toString());
    }


    /**
     * Gets the integer value for the specified key.
     * 
     * @param key the key
     * @return the value or null if it does not exist
     */
    public Integer getInt(final String key) {
        final Object value = getAttribute(key);

        if (value == null) {
            return null;
        }

        if (String.class.isAssignableFrom(value.getClass())) {
            return Integer.valueOf(value.toString());
        }

        return Integer.class.cast(value);
    }


    /**
     * Gets the boolean value for the specified key.
     * 
     * @param key the key
     * @return the value or null if it does not exist
     */
    public Boolean getBoolean(final String key) {
        final Object value = getAttribute(key);

        if (value == null) {
            return null;
        }

        if (String.class.isAssignableFrom(value.getClass())) {
            return Boolean.valueOf(value.toString());
        }

        return Boolean.class.cast(value);
    }


    /**
     * Gets the value for the specified key of the specified type.
     * 
     * @param <T> the value type
     * @param key the key
     * @param type the type of the value
     * @return the value or null if it does not exist
     */
    public <T> T getAttribute(final String key, final Class<T> type) {
        final Object value = getAttribute(key);

        if (value == null) {
            return null;
        }

        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }

        throw new ClassCastException("Attribute value " + value + " for key " + key + " cannot be cast to " + type.getName() + " because its of type " + value.getClass().getName());
    }

}
