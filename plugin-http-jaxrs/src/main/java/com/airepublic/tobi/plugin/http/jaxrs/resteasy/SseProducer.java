package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import javax.enterprise.inject.Produces;
import javax.ws.rs.sse.Sse;

import org.jboss.resteasy.plugins.providers.sse.SseImpl;

/**
 * Producer to create the {@link Sse} implementations for CDI injection.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SseProducer {
    private static Sse sse;


    /**
     * Produces the {@link Sse} instance.
     * 
     * @return the {@link Sse} instance
     */
    @Produces
    public Sse produceSse() {
        if (sse == null) {
            sse = new SseImpl();
        }

        return sse;
    }
}
