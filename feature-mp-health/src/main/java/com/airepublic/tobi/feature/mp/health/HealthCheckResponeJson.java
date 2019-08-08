package com.airepublic.tobi.feature.mp.health;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;

/**
 * Class to generate a Json response from {@link HealthCheck} instances.
 * 
 * @author Torsten
 *
 */
public class HealthCheckResponeJson {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger LOG;
    private static final Map<String, ?> JSON_CONFIG = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
    @Inject
    @Health
    private Instance<HealthCheck> healthChecks;
    @Inject
    @Liveness
    private Instance<HealthCheck> livenessChecks;
    @Inject
    @Readiness
    private Instance<HealthCheck> readinessChecks;


    /**
     * Writes the {@link HealthCheckJson} to the specified {@link OutputStream}.
     * 
     * @param out the {@link OutputStream}
     * @param health the {@link HealthCheckJson}
     */
    public void generate(final OutputStream out, final HealthCheckJson health) {
        final JsonWriterFactory factory = Json.createWriterFactory(JSON_CONFIG);
        final JsonWriter writer = factory.createWriter(out);

        writer.writeObject(health.getPayload());
        writer.close();
    }


    /**
     * Generates a {@link HealthCheckJson} of all annotated {@link Health}, {@link Liveness} and
     * {@link Readiness} instances.
     * 
     * @return the {@link HealthCheckJson}
     */
    public HealthCheckJson generate() {
        return toJson(healthChecks, livenessChecks, readinessChecks);
    }


    /**
     * Creates a {@link HealthCheckJson} for all the specified {@link HealthCheck} instances.
     * 
     * @param healthChecks the {@link HealthCheck} instances
     * @return the {@link HealthCheckJson}
     */
    @SafeVarargs
    private final HealthCheckJson toJson(final Instance<HealthCheck>... healthChecks) {
        final JsonArrayBuilder healthJsonArrayBuilder = Json.createArrayBuilder();
        final HealthCheckResponse.State status = HealthCheckResponse.State.UP;

        if (healthChecks != null) {
            for (final Instance<HealthCheck> instance : healthChecks) {
                for (final HealthCheck healthCheck : instance) {
                    final JsonObject healthCheckJson = toJson(healthCheck);
                    healthJsonArrayBuilder.add(healthCheckJson);
                }
            }
        }

        final JsonObjectBuilder builder = Json.createObjectBuilder();

        final JsonArray checkResults = healthJsonArrayBuilder.build();

        builder.add("status", checkResults.isEmpty() ? HealthCheckResponse.State.DOWN.name() : status.toString());
        builder.add("checks", checkResults);

        return new HealthCheckJson(builder.build());
    }


    /**
     * Creates a {@link HealthCheckJson} for the specified {@link HealthCheck}.
     * 
     * @param healthCheck the {@link HealthCheck}
     * @return the {@link JsonObject}
     */
    private JsonObject toJson(final HealthCheck healthCheck) {
        try {
            return toJson(healthCheck.call());
        } catch (final RuntimeException e) {
            LOG.log(Level.SEVERE, "Error processing Health Checks", e);

            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);

            final String stackTrace = stringWriter.getBuffer().toString();
            final HealthCheckResponseBuilder response = HealthCheckResponse.named(healthCheck.getClass().getName()).down();
            response.withData("Error", stackTrace);

            return toJson(response.build());
        }
    }


    /**
     * Creates a {@link JsonObject} for the specified {@link HealthCheckResponse}.
     * 
     * @param response the {@link HealthCheckResponse}
     * @return the {@link JsonObject}
     */
    private JsonObject toJson(final HealthCheckResponse response) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("name", response.getName());
        builder.add("state", response.getState().name());

        if (response.getData().isPresent()) {
            final JsonObjectBuilder dataBuilder = Json.createObjectBuilder(response.getData().get());

            builder.add("data", dataBuilder);
        }

        return builder.build();
    }

}