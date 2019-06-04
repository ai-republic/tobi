package com.airepublic.microprofile.module.http.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.ChannelAction;
import com.airepublic.microprofile.module.http.AsyncHttpRequestReader;
import com.airepublic.microprofile.module.http.Headers;
import com.airepublic.microprofile.module.http.HttpRequest;
import com.airepublic.microprofile.module.http.HttpResponse;
import com.airepublic.microprofile.module.http.HttpStatus;

public class HttpIOHandler extends AbstractIOHandler {
    private final AsyncHttpRequestReader requestReader = new AsyncHttpRequestReader();


    @Override
    protected void deploy() throws IOException {
    }


    @Override
    protected ChannelAction consume(final ByteBuffer buffer) throws IOException {
        return requestReader.receiveRequestBuffer(buffer);
    }


    @Override
    protected ChannelAction produce() throws IOException {
        final HttpResponse response = getHttpResponse();

        getSession().addToWriteBuffer(response.getHeaderBuffer(), response.getBody());

        return ChannelAction.CLOSE_ALL;
    }


    @Override
    protected void writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
    }


    @Override
    protected void writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
    }


    @Override
    protected ChannelAction onReadError(final Exception e) {
        return ChannelAction.CLOSE_ALL;
    }


    public HttpRequest getHttpRequest() throws IOException {
        return requestReader.getHttpRequest();
    }


    public HttpResponse getHttpResponse() {
        final Headers headers = new Headers();
        headers.add(Headers.CONTENT_TYPE, "text/plain");
        final ByteBuffer buffer = ByteBuffer.wrap("The page requested was not found.".getBytes());
        final HttpResponse response = new HttpResponse(HttpStatus.NOT_FOUND, headers, buffer);

        return response;
    }


    @Override
    protected void handleClosedInput() throws IOException {
        getSession().getKey().interestOpsOr(SelectionKey.OP_WRITE);
    }

}
