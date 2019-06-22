package com.airepublic.microprofile.module.http.jaxrs.resteasy;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.SynchronousExecutionContext;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.PathHelper;

import com.airepublic.microprofile.module.http.core.HttpRequest;

public class RestEasyHttpRequestWrapper implements org.jboss.resteasy.spi.HttpRequest {
    private final HttpRequest request;
    private final String contextPath;
    private final ResteasyAsynchronousContext restEasyContext;
    private final Map<String, Object> context = new HashMap<>();


    public RestEasyHttpRequestWrapper(final HttpRequest request, final org.jboss.resteasy.spi.HttpResponse response, final SynchronousDispatcher dispatcher, final String contextPath) {
        this.request = request;
        this.contextPath = contextPath;
        restEasyContext = new SynchronousExecutionContext(dispatcher, this, response);
    }


    @Override
    public HttpHeaders getHttpHeaders() {
        return new ResteasyHttpHeaders(getMutableHeaders());
    }


    @Override
    public MultivaluedMap<String, String> getMutableHeaders() {
        final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

        for (final Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            for (final String value : entry.getValue()) {
                map.add(entry.getKey(), value);
            }
        }

        return map;
    }


    @Override
    public InputStream getInputStream() {
        return null;
    }


    @Override
    public void setInputStream(final InputStream stream) {
        // TODO Auto-generated method stub

    }


    @Override
    public ResteasyUriInfo getUri() {
        return extractUriInfo(request);
    }


    private ResteasyUriInfo extractUriInfo(final HttpRequest request) {
        URI absoluteURI;
        try {
            absoluteURI = request.getUri();
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String path = PathHelper.getEncodedPathInfo(absoluteURI.getRawPath(), contextPath);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        URI baseURI = absoluteURI;
        if (!path.trim().equals("")) {
            String tmpContextPath = contextPath;

            if (!tmpContextPath.endsWith("/")) {
                tmpContextPath += "/";
            }

            baseURI = UriBuilder.fromUri(absoluteURI).replacePath(tmpContextPath).replaceQuery(null).build();
        } else {
            baseURI = UriBuilder.fromUri(absoluteURI).replaceQuery(null).build();
        }

        final URI relativeURI = UriBuilder.fromUri(path).replaceQuery(absoluteURI.getRawQuery()).build();
        // System.out.println("path: " + path);
        // System.out.println("query string: " + request.getQueryString());
        final ResteasyUriInfo uriInfo = new ResteasyUriInfo(baseURI, relativeURI);

        return uriInfo;
    }


    @Override
    public String getHttpMethod() {
        return request.getMethod();
    }


    @Override
    public void setHttpMethod(final String method) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void setRequestUri(final URI requestUri) throws IllegalStateException {
        request.setUri(requestUri);
    }


    @Override
    public void setRequestUri(final URI baseUri, final URI requestUri) throws IllegalStateException {
        URI uri;
        try {
            uri = new URI(baseUri.getPath() + requestUri.getPath());
            request.setUri(uri);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public MultivaluedMap<String, String> getFormParameters() {
        throw new UnsupportedOperationException();
    }


    @Override
    public MultivaluedMap<String, String> getDecodedFormParameters() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object getAttribute(final String attribute) {
        return context.get(attribute);
    }


    @Override
    public void setAttribute(final String name, final Object value) {
        context.put(name, value);
    }


    @Override
    public void removeAttribute(final String name) {
        context.remove(name);
    }


    @Override
    public Enumeration<String> getAttributeNames() {
        throw new UnsupportedOperationException();
    }


    @Override
    public ResteasyAsynchronousContext getAsyncContext() {
        return restEasyContext;
    }


    @Override
    public boolean isInitial() {
        return true;
    }


    @Override
    public void forward(final String path) {
    }


    @Override
    public boolean wasForwarded() {
        return false;
    }

}