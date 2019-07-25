package com.airepublic.microprofile.feature.logging.java;

import java.io.Serializable;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * A {@link Logger} which is {@link Serializable}. This logger can also be injected everywhere
 * including in a passivation scoped context like a SessionScoped context.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SerializableLogger extends Logger implements Serializable {
    private static final long serialVersionUID = 1L;


    public SerializableLogger(final String name) {
        this(Level.ALL, name, null);
    }


    public SerializableLogger(final Level level, final String name) {
        this(level, name, null);
    }


    public SerializableLogger(final Level level, final String name, final String bundleName) {
        super(name, bundleName);
        setLevel(level);

        final Formatter formatter = new DefaultFormatter();
        Stream.of(getHandlers()).forEach(h -> h.setFormatter(formatter));
    }

}
