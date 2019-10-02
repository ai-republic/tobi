package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson Provider for the custom configured {@link ObjectMapper} supporting the
 * {@link JavaTimeModule}.
 * 
 * @author Torsten Oltmanns
 *
 */
@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private final ObjectMapper defaultObjectMapper = createDefaultMapper();


    /**
     * Create the {@link ObjectMapper} supporting the {@link JavaTimeModule}.
     * 
     * @return the {@link ObjectMapper}
     */
    private static ObjectMapper createDefaultMapper() {
        final ObjectMapper result = new ObjectMapper();
        // result.configure(SerializationFeature.INDENT_OUTPUT, true);
        result.registerModule(new JavaTimeModule());
        result.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return result;
    }


    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return defaultObjectMapper;
    }
}