package com.airepublic.microprofile.module.http;

import java.nio.ByteBuffer;

import com.airepublic.microprofile.util.http.common.AbstractHttpIOHandler;
import com.airepublic.microprofile.util.http.common.Headers;
import com.airepublic.microprofile.util.http.common.HttpResponse;
import com.airepublic.microprofile.util.http.common.HttpStatus;

public class HttpIOHandler extends AbstractHttpIOHandler {
    private static final long serialVersionUID = 1L;


    /**
     * The default implementation will return a HTTP 404 {@link HttpResponse}.
     * 
     * @return a HTTP 404 {@link HttpResponse}
     */
    @Override
    public HttpResponse getHttpResponse() {
        final Headers headers = new Headers();
        headers.add(Headers.CONTENT_TYPE, "text/plain");
        final ByteBuffer buffer = ByteBuffer.wrap("The page requested was not found.".getBytes());
        final HttpResponse response = new HttpResponse(HttpStatus.NOT_FOUND, headers, buffer);

        return response;
    }

}
