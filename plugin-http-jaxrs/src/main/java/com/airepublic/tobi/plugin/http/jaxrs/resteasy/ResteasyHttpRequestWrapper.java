package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.SynchronousExecutionContext;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.PathHelper;

import com.airepublic.http.common.HttpRequest;

/**
 * Wrapper for the {@link HttpRequest} as a {@link org.jboss.resteasy.spi.HttpRequest}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ResteasyHttpRequestWrapper implements org.jboss.resteasy.spi.HttpRequest {
    private final HttpRequest request;
    private final String contextPath;
    private final ResteasyAsynchronousContext restEasyContext;
    private final Map<String, Object> attributes = new HashMap<>();
    private InputStream is;
    private ResteasyUriInfo uriInfo;


    /**
     * Constructor.
     * 
     * @param request the {@link HttpRequest} to be wrapped
     * @param response the {@link HttpResponse}
     * @param dispatcher the {@link SynchronousDispatcher}
     * @param contextPath the context path of the {@link Application}
     */
    public ResteasyHttpRequestWrapper(final HttpRequest request, final HttpResponse response, final SynchronousDispatcher dispatcher, final String contextPath) {
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
        if (is == null) {
            final byte[] buf = new byte[request.getBody().remaining()];

            request.getBody().mark();
            request.getBody().get(buf);
            request.getBody().reset();

            is = new ByteArrayInputStream(buf);
        }

        return is;
    }


    @Override
    public void setInputStream(final InputStream stream) {
        is = stream;
    }


    @Override
    public ResteasyUriInfo getUri() {
        if (uriInfo == null) {
            uriInfo = extractUriInfo();
        }

        return uriInfo;
    }


    /**
     * Extracts the {@link ResteasyUriInfo} from the {@link HttpRequest}.
     * 
     * @param request the {@link HttpRequest}
     * @return the {@link ResteasyUriInfo}
     */
    private ResteasyUriInfo extractUriInfo() {
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
        return attributes.get(attribute);
    }


    @Override
    public void setAttribute(final String name, final Object value) {
        attributes.put(name, value);
    }


    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
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