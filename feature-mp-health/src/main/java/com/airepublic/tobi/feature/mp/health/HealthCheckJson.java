package com.airepublic.tobi.feature.mp.health;

import javax.json.JsonObject;

import org.eclipse.microprofile.health.HealthCheckResponse;

public class HealthCheckJson {

    private final JsonObject payload;


    public HealthCheckJson(final JsonObject payload) {
        this.payload = payload;
    }


    public JsonObject getPayload() {
        return payload;
    }


    public boolean isDown() {
        return HealthCheckResponse.State.DOWN.toString().equals(payload.getString("status"));
    }
}
