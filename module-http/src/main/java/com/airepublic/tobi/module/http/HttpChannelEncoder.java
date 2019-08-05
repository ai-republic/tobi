package com.airepublic.tobi.module.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.airepublic.http.common.AsyncHttpReader;
import com.airepublic.http.common.HttpRequest;
import com.airepublic.http.common.SslSupport;
import com.airepublic.tobi.core.spi.Attributes;
import com.airepublic.tobi.core.spi.IChannelEncoder;
import com.airepublic.tobi.core.spi.Pair;
import com.airepublic.tobi.core.spi.Request;

public class HttpChannelEncoder implements AutoCloseable, IChannelEncoder {
    public static final String HEADERS = "http.headers";
    public static final String REQUEST_LINE = "http.request.requestLine";
    private SSLEngine sslEngine;
    private boolean isSecure;
    private SocketChannel channel;
    private final AsyncHttpReader httpReader = new AsyncHttpReader();


    public HttpChannelEncoder() {
    }


    public void init(final SocketChannel channel, final SSLContext sslContext, final boolean isSecure) throws IOException {
        this.channel = channel;
        this.isSecure = isSecure;

        // create SSL engine if necessary
        if (isSecure) {
            sslEngine = SslSupport.serverSSLHandshake(sslContext, channel);
        }
    }


    public SSLEngine getSslEngine() {
        return sslEngine;
    }


    @Override
    public Pair<Status, Request> decode(ByteBuffer buffer) throws IOException {
        if (isSecure) {
            buffer = SslSupport.unwrap(sslEngine, channel, buffer);
        }

        if (httpReader.receiveBuffer(buffer)) {
            final HttpRequest request = httpReader.getHttpRequest();
            httpReader.clear();

            final Attributes attributes = new Attributes();
            attributes.set(REQUEST_LINE, request.getRequestLine());
            attributes.set(HEADERS, request.getHeaders());
            final Request wrapped = new Request(attributes, request.getBody());
            return new Pair<>(Status.FULLY_READ, wrapped);
        }

        return new Pair<>(Status.NEED_MORE_DATA, null);
    }


    @Override
    public ByteBuffer[] encode(ByteBuffer... buffers) throws IOException {
        if (isSecure) {
            buffers = SslSupport.wrap(sslEngine, channel, buffers);
        }

        return buffers;
    }


    @Override
    public void close() {
        if (sslEngine != null) {
            try {
                SslSupport.closeConnection(channel, sslEngine);
            } catch (final Exception e) {
            }
        }
    }
}
