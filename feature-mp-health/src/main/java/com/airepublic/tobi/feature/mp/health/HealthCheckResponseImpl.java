package com.airepublic.tobi.feature.mp.health;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * The implementation of the {@link HealthCheckResponse}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class HealthCheckResponseImpl extends HealthCheckResponse {
    private final String name;
    private final State state;
    private final Map<String, Object> data;


    /**
     * Constructor.
     * 
     * @param name the name of the health check
     * @param state the state
     * @param data heath check data
     */
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
