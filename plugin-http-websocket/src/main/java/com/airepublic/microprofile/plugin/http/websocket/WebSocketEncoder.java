package com.airepublic.microprofile.plugin.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import com.airepublic.microprofile.core.spi.IChannelEncoder;
import com.airepublic.microprofile.core.spi.IRequest;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.Pair;
import com.airepublic.microprofile.core.spi.SessionConstants;
import com.airepublic.microprofile.util.http.common.SslSupport;

public class WebSocketEncoder implements IChannelEncoder {
    private final IServerSession session;

    public static class WebSocketRequest implements IRequest {
        private final ByteBuffer buffer;


        public WebSocketRequest(final ByteBuffer buffer) {
            this.buffer = buffer;
        }


        @Override
        public Map<?, ?> getAttributes() {
            return Collections.emptyMap();
        }


        @Override
        public ByteBuffer getPayload() {
            return buffer;
        }

    }


    public WebSocketEncoder(final IServerSession session) {
        this.session = session;
    }


    @Override
    public Pair<Status, IRequest> decode(final ByteBuffer buffer) throws IOException {
        final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_SSL_ENGINE, SSLEngine.class);
        final ByteBuffer unwrappedBuffer = SslSupport.unwrap(sslEngine, session.getChannel(), buffer);
        return new Pair<>(Status.FULLY_READ, new WebSocketRequest(unwrappedBuffer));
    }


    @Override
    public ByteBuffer[] encode(final ByteBuffer... buffers) throws IOException {
        final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_SSL_ENGINE, SSLEngine.class);

        return SslSupport.wrap(sslEngine, buffers);
    }


    @Override
    public void close() throws Exception {
    }

}
