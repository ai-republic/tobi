package com.airepublic.microprofile.plugin.http.sse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.airepublic.microprofile.util.http.common.Headers;

public class SseWebTarget implements WebTarget {
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<Object, Map<Class<?>, Integer>> components = new HashMap<>();
    private final static Class<?> noContractClass = Class.class;
    private final UriBuilder uriBuilder;


    public SseWebTarget(final URI uri) {
        uriBuilder = new SseUriBuilder();
        uriBuilder.uri(uri);
        uriBuilder.port(uri.getPort());
    }


    public SseWebTarget(final UriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }


    @Override
    public Configuration getConfiguration() {
        return null;
    }


    @Override
    public WebTarget property(final String name, final Object value) {
        properties.put(name, value);
        return this;
    }


    @Override
    public WebTarget register(final Class<?> componentClass) {
        return register(componentClass, new HashMap<>());
    }


    @Override
    public WebTarget register(final Class<?> componentClass, final int priority) {
        final Map<Class<?>, Integer> contracts = new HashMap<>();
        contracts.put(noContractClass, priority);
        register(componentClass, contracts);
        return this;
    }


    @Override
    public WebTarget register(final Class<?> componentClass, final Class<?>... contracts) {
        final Map<Class<?>, Integer> map = new HashMap<>();

        for (final Class<?> contract : contracts) {
            map.put(contract, 0);
        }

        register(componentClass, contracts);
        return this;
    }


    @Override
    public WebTarget register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
        try {
            register(componentClass.getConstructor().newInstance(), contracts);
        } catch (final Exception e) {
            throw new RuntimeException("Error registering component class " + componentClass, e);
        }
        return this;
    }


    @Override
    public WebTarget register(final Object component) {
        return register(component, new HashMap<>());
    }


    @Override
    public WebTarget register(final Object component, final int priority) {
        final Map<Class<?>, Integer> contracts = new HashMap<>();
        contracts.put(noContractClass, priority);
        register(component, contracts);
        return this;
    }


    @Override
    public WebTarget register(final Object component, final Class<?>... contracts) {
        final Map<Class<?>, Integer> map = new HashMap<>();

        for (final Class<?> contract : contracts) {
            map.put(contract, 0);
        }

        register(component, contracts);
        return this;
    }


    @Override
    public WebTarget register(final Object component, final Map<Class<?>, Integer> contracts) {
        components.put(component, contracts);
        return this;
    }


    @Override
    public URI getUri() {
        return uriBuilder.clone().build();
    }


    @Override
    public UriBuilder getUriBuilder() {
        return uriBuilder;
    }


    @Override
    public WebTarget path(final String path) {
        return new SseWebTarget(uriBuilder.clone().path(path));
    }


    @Override
    public WebTarget resolveTemplate(final String name, final Object value) throws NullPointerException {
        return new SseWebTarget(uriBuilder.resolveTemplate(name, value));
    }


    @Override
    public WebTarget resolveTemplates(final Map<String, Object> templateValues) throws NullPointerException {
        return new SseWebTarget(uriBuilder.resolveTemplates(templateValues));
    }


    @Override
    public WebTarget resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) throws NullPointerException {
        return new SseWebTarget(uriBuilder.resolveTemplate(name, value, encodeSlashInPath));
    }


    @Override
    public WebTarget resolveTemplateFromEncoded(final String name, final Object value) throws NullPointerException {
        return new SseWebTarget(uriBuilder.resolveTemplateFromEncoded(name, value));
    }


    @Override
    public WebTarget resolveTemplatesFromEncoded(final Map<String, Object> templateValues) throws NullPointerException {
        return new SseWebTarget(uriBuilder.resolveTemplatesFromEncoded(templateValues));
    }


    @Override
    public WebTarget resolveTemplates(final Map<String, Object> templateValues, final boolean encodeSlashInPath) throws NullPointerException {
        return new SseWebTarget(uriBuilder.resolveTemplates(templateValues, encodeSlashInPath));
    }


    @Override
    public WebTarget matrixParam(final String name, final Object... values) {
        UriBuilder clone = uriBuilder.clone();

        if (values.length == 1 && values[0] == null) {
            clone.replaceMatrixParam(name, null);
        } else {
            clone = clone.matrixParam(name, values);
        }

        return new SseWebTarget(clone);
    }


    @Override
    public WebTarget queryParam(final String name, final Object... values) {
        final UriBuilder clone = uriBuilder.clone();

        if (values == null || values.length == 0) {
            clone.replaceQueryParam(name, values);
        }

        clone.queryParam(name, values);

        return new SseWebTarget(clone);
    }


    @Override
    public Invocation.Builder request() {
        final SseInvocationBuilder builder = new SseInvocationBuilder(uriBuilder.build());
        builder.setTarget(this);
        return builder;
    }


    @Override
    public Invocation.Builder request(final String... acceptedResponseTypes) {
        final SseInvocationBuilder builder = new SseInvocationBuilder(uriBuilder.build());
        builder.getHeaders().add(Headers.ACCEPT, acceptedResponseTypes);
        builder.setTarget(this);
        return builder;
    }


    @Override
    public Invocation.Builder request(final MediaType... acceptedResponseTypes) {
        final SseInvocationBuilder builder = new SseInvocationBuilder(uriBuilder.build());
        builder.getHeaders().add(Headers.ACCEPT, Stream.of(acceptedResponseTypes).map(m -> m.toString()).toArray(String[]::new));
        builder.setTarget(this);
        return builder;
    }
}
