package com.airepublic.microprofile.feature.mp.health;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheckResponse;

class HealthCheckResponseImpl extends HealthCheckResponse {
    private final String name;
    private final State state;
    private final Map<String, Object> data;


    HealthCheckResponseImpl(final String name, final State state, final Map<String, Object> data) {
        this.name = name;
        this.state = state;
        this.data = data;
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public State getState() {
        return state;
    }


    @Override
    public Optional<Map<String, Object>> getData() {
        return Optional.ofNullable(data);
    }

}
