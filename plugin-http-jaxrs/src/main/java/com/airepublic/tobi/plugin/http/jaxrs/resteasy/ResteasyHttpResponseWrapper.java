package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpStatus;
import com.airepublic.tobi.module.http.HttpResponse;

/**
 * The wrapper for the {@link HttpResponse} to a {@link org.jboss.resteasy.spi.HttpResponse}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ResteasyHttpResponseWrapper implements org.jboss.resteasy.spi.HttpResponse {
    private final HttpResponse response;
    private final MultivaluedHashMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
    private final ByteArrayOutputStream rawOutputStream = new ByteArrayOutputStream();
    private OutputStream outputStream = rawOutputStream;
    private boolean committed = false;
    private final ResteasyIOHandler handler;


    /**
     * Constructor.
     * 
     * @param response the {@link HttpResponse} to be wrapped
     * @param handler the {@link ResteasyIOHandler}
     */
    public ResteasyHttpResponseWrapper(final HttpResponse response, final ResteasyIOHandler handler) {
        this.response = response;
        this.handler = handler;
        response.getHeaders().entrySet().forEach(e -> e.getValue().forEach(v -> responseHeaders.add(e.getKey(), v)));
    }


    @Override
    public int getStatus() {
        return response.getStatus().code();
    }


    @Override
    public void setStatus(final int status) {
        response.withStatus(HttpStatus.forCode(status));
    }


    @Override
    public MultivaluedMap<String, Object> getOutputHeaders() {
        return responseHeaders;
    }


    @Override
    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }


    public ByteArrayOutputStream getRawOutputStream() {
        return rawOutputStream;
    }


    @Override
    public void setOutputStream(final OutputStream os) {
        outputStream = os;
    }


    @Override
    public void addNewCookie(final NewCookie cookie) {
        // TODO Auto-generated method stub

    }


    @Override
    public void sendError(final int status) throws IOException {
        sendError(status, "");
    }


    @Override
    public void sendError(final int status, final String message) throws IOException {
        handler.setHttpResponse(new HttpResponse(HttpStatus.forCode(status), null, ByteBuffer.wrap(message.getBytes())));
        committed = true;
    }


    @Override
    public boolean isCommitted() {
        return committed;
    }


    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void flushBuffer() throws IOException {
        outputStream.flush();
    }


    /**
     * Merges the {@link org.jboss.resteasy.spi.HttpResponse} to the {@link HttpResponse}.
     */
    public void mergeToResponse() {
        response.withHeaders(new Headers());
        responseHeaders.keySet().stream().forEach(key -> responseHeaders.get(key).stream().forEach(value -> response.getHeaders().add(key, value.toString())));

        final byte[] bytes = getRawOutputStream().toByteArray();
        response.withBody(ByteBuffer.wrap(bytes));
    }

}