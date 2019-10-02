package com.airepublic.tobi.module.http;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpStatus;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.Pair;

/**
 * A default {@link IIOHandler} implementation for HTTP which will return a 404.
 * 
 * @author Torsten Oltmanns
 *
 */
public class HttpIOHandler extends AbstractHttpIOHandler {
    private static final long serialVersionUID = 1L;


    /**
     * The default implementation will return a HTTP 404 {@link HttpResponse}.
     * 
     * @return a HTTP 404 {@link HttpResponse}
     */
    @Override
    protected Pair<HttpResponse, CompletionHandler<?, ?>> getHttpResponse() {
        final Headers headers = new Headers();
        headers.add(Headers.CONTENT_TYPE, "text/plain");
        final ByteBuffer buffer = ByteBuffer.wrap("The page requested was not found.".getBytes());
        final HttpResponse response = new HttpResponse(HttpStatus.NOT_FOUND, headers, buffer);

        return new Pair<>(response, null);
    }
}
