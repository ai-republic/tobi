package com.airepublic.tobi.module.http;

import java.net.URI;
import java.nio.ByteBuffer;

import com.airepublic.http.common.Headers;
import com.airepublic.tobi.core.spi.IRequest;
import com.airepublic.tobi.core.spi.IServerSession;

/**
 * The {@link IRequest} implementation of the {@link com.airepublic.http.common.HttpRequest}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class HttpRequest extends com.airepublic.http.common.HttpRequest implements IRequest {
    private static final long serialVersionUID = 1L;
    private final IServerSession session;


    /**
     * Constructor.
     * 
     * @param session the {@link IServerSession}
     * @param uri the {@link URI}
     */
    public HttpRequest(final IServerSession session, final URI uri) {
        super(uri, new Headers());
        this.session = session;
    }


    /**
     * Constructor.
     * 
     * @param session the {@link IServerSession}
     * @param uri the {@link URI}
     * @param headers the {@link Headers} associated with this request.
     */
    public HttpRequest(final IServerSession session, final URI uri, final Headers headers) {
        super(uri, headers);
        this.session = session;
    }


    /**
     * Constructor.
     * 
     * @param session the {@link IServerSession}
     * @param headers the {@link Headers} associated with this request.
     * @param requestLine the requestLine
     */
    public HttpRequest(final IServerSession session, final String requestLine, final Headers headers) {
        super(requestLine, headers, null);
        this.session = session;
    }


    /**
     * Constructor.
     * 
     * @param session the {@link IServerSession}
     * @param headers the {@link Headers} associated with this request.
     * @param requestLine the requestLine
     * @param body the body {@link ByteBuffer}
     */
    public HttpRequest(final IServerSession session, final String requestLine, final Headers headers, final ByteBuffer body) {
        super(requestLine, headers, body);
        this.session = session;
    }


    /**
     * Gets the associated {@link IServerSession}.
     * 
     * @return the associated {@link IServerSession}
     */
    @Override
    public IServerSession getSession() {
        return session;
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


    /**
     * Gets the requests body.
     * 
     * @return the {@link ByteBuffer} containing the body
     */
    @Override
    public ByteBuffer getPayload() {
        return getBody();
    }


    /**
     * Sets the requests body.
     * 
     * @param payload the {@link ByteBuffer} containing the body
     */
    @Override
    public void setPayload(final ByteBuffer payload) {
        setBody(payload);
    }
}
