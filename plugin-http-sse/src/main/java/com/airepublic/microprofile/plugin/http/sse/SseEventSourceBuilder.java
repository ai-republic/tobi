package com.airepublic.microprofile.plugin.http.sse;

import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

/**
 * The service provider implementation of the {@link Builder} to create a {@link SseEventSource}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SseEventSourceBuilder extends SseEventSource.Builder {
    private WebTarget endpoint;
    private long reconnectionDelay;


    @Override
    protected Builder target(final WebTarget endpoint) {
        this.endpoint = endpoint;
        return this;
    }


    @Override
    public Builder reconnectingEvery(final long delay, final TimeUnit unit) {
        reconnectionDelay = unit.toMillis(delay);
        return this;
    }


    @Override
    public SseEventSource build() {
        final SseEventSourceImpl source = CDI.current().select(SseEventSourceImpl.class).get();
        source.setEndpoint(endpoint);
        source.setReconnectionDelay(reconnectionDelay);
        return source;
    }

}
