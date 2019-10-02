package com.airepublic.tobi.feature.mp.config;

import java.util.Objects;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Priorization wrapper to order {@link Converter}s by their priority.
 * 
 * @author Torsten Oltmanns
 *
 */
public class PriorizedConverter {
    private final Converter<?> converter;
    private final int priority;


    /**
     * Constructor.
     * 
     * @param converter the {@link Converter}
     * @param priority its priority
     */
    public PriorizedConverter(final Converter<?> converter, final int priority) {
        this.converter = converter;
        this.priority = priority;
    }


    /**
     * Gets the priority.
     * 
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }


    /**
     * Gets the {@link Converter}.
     * 
     * @return the {@link Converter}
     */
    public Converter<?> getConverter() {
        return converter;
    }


    @Override
    public int hashCode() {
        return Objects.hash(converter, priority);
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PriorizedConverter other = (PriorizedConverter) obj;
        return Objects.equals(converter, other.converter) && priority == other.priority;
    }

}
