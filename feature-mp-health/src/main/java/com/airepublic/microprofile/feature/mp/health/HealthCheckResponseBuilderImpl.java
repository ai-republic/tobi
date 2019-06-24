package com.airepublic.microprofile.feature.mp.health;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

class HealthCheckResponseBuilderImpl extends HealthCheckResponseBuilder {
    private String name;
    private HealthCheckResponse.State state = HealthCheckResponse.State.DOWN;
    private final Map<String, Object> data = new HashMap<>();


    @Override
    public HealthCheckResponseBuilder name(final String name) {
        this.name = name;
        return this;
    }


    @Override
    public HealthCheckResponseBuilder withData(final String key, final String value) {
        data.put(key, value);
        return this;
    }


    @Override
    public HealthCheckResponseBuilder withData(final String key, final long value) {
        data.put(key, value);
        return this;
    }


    @Override
    public HealthCheckResponseBuilder withData(final String key, final boolean value) {
        data.put(key, value);
        return this;
    }


    @Override
    public HealthCheckResponseBuilder up() {
        state = HealthCheckResponse.State.UP;
        return this;
    }


    @Override
    public HealthCheckResponseBuilder down() {
        state = HealthCheckResponse.State.DOWN;
        return this;
    }


    @Override
    public HealthCheckResponseBuilder state(final boolean up) {
        if (up) {
            return up();
        }

        return down();
    }


    @Override
    public HealthCheckResponse build() {
        if (null == name || name.trim().length() == 0) {
            throw new IllegalArgumentException("Health Check contains an invalid name. Can not be null or empty.");
        }

        return new HealthCheckResponseImpl(name, state, data.isEmpty() ? null : data);
    }
}