package com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

import javax.enterprise.inject.Produces;
import javax.ws.rs.sse.Sse;

import org.jboss.resteasy.plugins.providers.sse.SseImpl;

public class SseProducer {
    private static Sse sse;


    @Produces
    public Sse produceSse() {
        if (sse == null) {
            sse = new SseImpl();
        }

        return sse;
    }
}
