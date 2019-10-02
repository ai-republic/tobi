package com.airepublic.tobi.feature.mp.health;

import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

/**
 * The implementation of the {@link HealthCheckResponseProvider}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class HealthCheckResponseProviderImpl implements HealthCheckResponseProvider {

    @Override
    public HealthCheckResponseBuilder createResponseBuilder() {
        return new HealthCheckResponseBuilderImpl();
    }
}