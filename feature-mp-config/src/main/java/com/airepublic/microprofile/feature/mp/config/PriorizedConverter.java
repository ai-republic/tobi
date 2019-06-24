package com.airepublic.microprofile.feature.mp.config;

import org.eclipse.microprofile.config.spi.Converter;

public class PriorizedConverter {
    private final Converter<?> converter;
    private final int priority;


    public PriorizedConverter(final Converter<?> converter, final int priority) {
        this.converter = converter;
        this.priority = priority;
    }


    public int getPriority() {
        return priority;
    }


    public Converter<?> getConverter() {
        return converter;
    }

}
