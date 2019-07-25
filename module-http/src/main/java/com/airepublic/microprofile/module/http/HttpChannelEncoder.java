package com.airepublic.microprofile.module.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.airepublic.microprofile.core.spi.IChannelEncoder;
import com.airepublic.microprofile.core.spi.IRequest;
import com.airepublic.microprofile.core.spi.Pair;
import com.airepublic.microprofile.util.http.common.AsyncHttpReader;
import com.airepublic.microprofile.util.http.common.HttpRequest;
import com.airepublic.microprofile.util.http.common.SslSupport;

public class HttpChannelEncoder implements AutoCloseable, IChannelEncoder {
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
    public Pair<Status, IRequest> decode(ByteBuffer buffer) throws IOException {
        if (isSecure) {
            buffer = SslSupport.unwrap(sslEngine, channel, buffer);
        }

        if (httpReader.receiveBuffer(buffer)) {
            final HttpRequest request = httpReader.getHttpRequest();
            httpReader.clear();
            return new Pair<>(Status.FULLY_READ, request);
        }

        return new Pair<>(Status.NEED_MORE_DATA, null);
    }


    @Override
    public ByteBuffer[] encode(ByteBuffer... buffers) throws IOException {
        if (isSecure) {
            buffers = SslSupport.wrap(sslEngine, buffers);
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
