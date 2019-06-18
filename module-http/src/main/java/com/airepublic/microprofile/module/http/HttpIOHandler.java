package com.airepublic.microprofile.module.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.airepublic.microprofile.module.http.core.AbstractHttpIOHandler;
import com.airepublic.microprofile.module.http.core.Headers;
import com.airepublic.microprofile.module.http.core.HttpResponse;
import com.airepublic.microprofile.module.http.core.HttpStatus;

public class HttpIOHandler extends AbstractHttpIOHandler {

    @Override
    protected void deploy() throws IOException {
    }


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
