package com.airepublic.tobi.module.http;

import java.nio.ByteBuffer;

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpStatus;
import com.airepublic.tobi.core.spi.IResponse;

/**
 * The {@link IResponse} implementation of the {@link com.airepublic.http.common.HttpResponse}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class HttpResponse extends com.airepublic.http.common.HttpResponse implements IResponse {

    /**
     * Constructor.
     */
    public HttpResponse() {
    }


    /**
     * Constructor.
     * 
     * @param status the response {@link HttpStatus}
     */
    public HttpResponse(final HttpStatus status) {
        this(status, new Headers(), null);
    }


    /**
     * Constructor.
     * 
     * @param status the response {@link HttpStatus}
     * @param headers the response {@link Headers}
     */
    public HttpResponse(final HttpStatus status, final Headers headers) {
        this(status, headers, null);
    }


    /**
     * Constructor.
     * 
     * @param status the response {@link HttpStatus}
     * @param headers the response {@link Headers}
     * @param body the response body as {@link ByteBuffer}
     */
    public HttpResponse(final HttpStatus status, final Headers headers, final ByteBuffer body) {
        super(status, headers, body);
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String key) {
        return (T) getHeaders().get(key);
    }


    @Override
    public <T> void setAttribute(final String key, final T value) {
        getHeaders().add(key, value.toString());
    }


    @Override
    public ByteBuffer getAttributesBuffer() {
        return getHeaderBuffer();
    }


    @Override
    public ByteBuffer getPayload() {
        return getBody();
    }


    /**
     * Sets the body fluently.
     * 
     * @param body the body
     * @return this {@link HttpResponse}
     */
    public HttpResponse withBody(final ByteBuffer body) {
        setBody(body);

        return this;
    }


    /**
     * Sets a header fluently.
     * 
     * @param name the header name
     * @param values the header values
     * @return this {@link HttpResponse}
     */
    public HttpResponse withHeader(final String name, final String... values) {
        if (getHeaders() == null) {
            setHeaders(new Headers());
        }

        getHeaders().add(name, values);

        return this;
    }


    /**
     * Sets the {@link Headers} fluently.
     * 
     * @param headers the {@link Headers}
     * @return this {@link HttpResponse}
     */
    public HttpResponse withHeaders(final Headers headers) {
        setHeaders(headers);

        return this;
    }


    /**
     * Sets the scheme fluently.
     * 
     * @param scheme the scheme
     * @return this {@link HttpResponse}
     */
    public HttpResponse withScheme(final String scheme) {
        setScheme(scheme);

        return this;
    }


    /**
     * Sets the {@link HttpStatus} fluently.
     * 
     * @param status the {@link HttpStatus}
     * @return this {@link HttpResponse}
     */
    public HttpResponse withStatus(final HttpStatus status) {
        setStatus(status);

        return this;
    }


    /**
     * Sets the HTTP version fluently.
     * 
     * @param version the HTTP version
     * @return this {@link HttpResponse}
     */
    public HttpResponse withVersion(final String version) {
        setVersion(version);

        return this;
    }

}
