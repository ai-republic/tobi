package com.airepublic.tobi.feature.mp.health;

import javax.json.JsonObject;

import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Wrapper to receive Json responses.
 * 
 * @author Torsten Oltmanns
 *
 */
public class HealthCheckJson {
    private final JsonObject payload;


    /**
     * Constructor.
     * 
     * @param payload the Json response
     */
    public HealthCheckJson(final JsonObject payload) {
        this.payload = payload;
    }


    /**
     * Gets the Json response.
     * 
     * @return the {@link JsonObject}
     */
    public JsonObject getPayload() {
        return payload;
    }


    /**
     * Returns true if the response has the status {@link HealthCheckResponse.State#DOWN}.
     * 
     * @return true if down
     */
    public boolean isDown() {
        return HealthCheckResponse.State.DOWN.toString().equals(payload.getString("status"));
    }
}
