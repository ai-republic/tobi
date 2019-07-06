package com.airepublic.microprofile.plugin.http.sse;

import java.net.URI;
import java.util.Locale;
import java.util.stream.Stream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.airepublic.microprofile.util.http.common.CookieParser;
import com.airepublic.microprofile.util.http.common.Headers;

public class SseInvocationBuilder implements Invocation.Builder {
    protected SseInvocation invocation;
    private final URI uri;
    private WebTarget target;
    private Headers headers;


    public SseInvocationBuilder(final URI uri) {
        invocation = new SseInvocation(uri, new Headers());
        this.uri = uri;
    }


    @Override
    public Invocation.Builder accept(final String... mediaTypes) {
        headers.add(Headers.ACCEPT, mediaTypes);
        return this;
    }


    @Override
    public Invocation.Builder accept(final MediaType... mediaTypes) {
        Stream.of(mediaTypes).forEach(mt -> headers.add(Headers.ACCEPT, mt.toString()));
        return this;
    }


    @Override
    public Invocation.Builder acceptLanguage(final Locale... locales) {
        Stream.of(locales).forEach(locale -> headers.add(Headers.ACCEPT_LANGUAGE, locale.toLanguageTag()));
        return this;
    }


    @Override
    public Invocation.Builder acceptLanguage(final String... locales) {
        headers.add(Headers.ACCEPT_LANGUAGE, locales);
        return this;
    }


    @Override
    public Invocation.Builder acceptEncoding(final String... encodings) {
        headers.add(Headers.ACCEPT_ENCODING, encodings);
        return this;
    }


    @Override
    public Invocation.Builder cookie(final Cookie cookie) {
        final StringBuffer buffer = new StringBuffer();
        CookieParser.appendCookieValue(buffer, cookie.getVersion(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), null, -1, false);
        headers.add(Headers.COOKIE, buffer.toString());
        return this;
    }


    @Override
    public Invocation.Builder cookie(final String name, final String value) {
        cookie(new Cookie(name, value));
        return this;
    }


    @Override
    public Invocation.Builder cacheControl(final CacheControl cacheControl) {
        headers.add(Headers.CACHE_CONTROL, "no-store");
        return this;
    }


    @Override
    public Invocation.Builder header(final String name, final Object value) {
        headers.add(name, value.toString());
        return this;
    }


    @Override
    public Invocation.Builder headers(final MultivaluedMap<String, Object> headers) {
        headers.entrySet().stream().forEach(e -> headers.add(e.getKey(), e.getValue().toString()));
        return this;
    }


    @Override
    public Invocation build(final String method) {
        return build(method, null);
    }


    @Override
    public Invocation build(final String method, final Entity<?> entity) {
        invocation.setMethod(method);
        invocation.setEntity(entity);
        return invocation;
    }


    @Override
    public Invocation buildGet() {
        return build(HttpMethod.GET);
    }


    @Override
    public Invocation buildDelete() {
        return build(HttpMethod.DELETE);
    }


    @Override
    public Invocation buildPost(final Entity<?> entity) {
        return build(HttpMethod.POST, entity);
    }


    @Override
    public Invocation buildPut(final Entity<?> entity) {
        return build(HttpMethod.PUT, entity);
    }


    @Override
    public AsyncInvoker async() {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response head() {
        return build(HttpMethod.HEAD).invoke();
    }


    @Override
    public Invocation.Builder property(final String name, final Object value) {
        invocation.property(name, value);
        return this;
    }


    @Override
    public CompletionStageRxInvoker rx() {
        throw new UnsupportedOperationException("RX not implemented for SSE");
    }


    @Override
    public <T extends RxInvoker> T rx(final Class<T> clazz) {
        throw new UnsupportedOperationException("RX not implemented for SSE");
    }


    public void setTarget(final WebTarget target) {
        this.target = target;
    }


    public Headers getHeaders() {
        return headers;
    }


    @Override
    public Response get() {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T get(final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T get(final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response put(final Entity<?> entity) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T put(final Entity<?> entity, final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T put(final Entity<?> entity, final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response post(final Entity<?> entity) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T post(final Entity<?> entity, final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T post(final Entity<?> entity, final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response delete() {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T delete(final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T delete(final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response options() {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T options(final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T options(final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response trace() {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T trace(final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T trace(final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response method(final String name) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T method(final String name, final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T method(final String name, final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Response method(final String name, final Entity<?> entity) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T method(final String name, final Entity<?> entity, final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> T method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }

}
