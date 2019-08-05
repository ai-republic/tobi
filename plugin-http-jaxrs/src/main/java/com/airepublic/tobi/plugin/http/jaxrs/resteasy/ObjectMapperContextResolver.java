package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    final ObjectMapper defaultObjectMapper;


    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return defaultObjectMapper;
    }


    private static ObjectMapper createDefaultMapper() {
        final ObjectMapper result = new ObjectMapper();
        // result.configure(SerializationFeature.INDENT_OUTPUT, true);
        result.registerModule(new JavaTimeModule());
        result.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return result;
    }


    public ObjectMapperContextResolver() {
        defaultObjectMapper = createDefaultMapper();
    }

}