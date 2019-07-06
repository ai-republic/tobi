package com.airepublic.microprofile.plugin.http.sse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.airepublic.microprofile.util.http.common.Headers;

public class SseInvocation implements Invocation {
    protected Headers headers;
    protected String method;
    protected Object entity;
    protected Type entityGenericType;
    protected Class<?> entityClass;
    protected Annotation[] entityAnnotations;
    protected URI uri;


    public SseInvocation(final URI uri, final Headers headers) {
        this.uri = uri;
        this.headers = headers;
    }


    @Override
    public Response invoke() {
        throw new UnsupportedOperationException("SSE only supports one-way async calls");
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(final Class<T> responseType) {
        throw new UnsupportedOperationException("SSE only supports one-way async calls");
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(final GenericType<T> responseType) {
        throw new UnsupportedOperationException("SSE only supports one-way async calls");
    }


    @Override
    public Future<Response> submit() {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> Future<T> submit(final Class<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public <T> Future<T> submit(final GenericType<T> responseType) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> Future<T> submit(final InvocationCallback<T> callback) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    @Override
    public Invocation property(final String name, final Object value) {
        throw new UnsupportedOperationException("Not implemented for SSE");
    }


    public void setMethod(final String method) {
        this.method = method;
    }


    public void setEntity(final Entity<?> entity) {
        this.entity = entity;
    }

}
