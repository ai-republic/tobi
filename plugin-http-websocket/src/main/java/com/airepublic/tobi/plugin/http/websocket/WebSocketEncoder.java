package com.airepublic.tobi.plugin.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

import com.airepublic.http.common.SslSupport;
import com.airepublic.tobi.core.spi.IChannelEncoder;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Pair;
import com.airepublic.tobi.core.spi.Request;
import com.airepublic.tobi.core.spi.SessionConstants;

/**
 * The {@link IChannelEncoder} implementation for the websocket requests/responses supporting SSL.
 * 
 * @author Torsten Oltmanns
 *
 */
public class WebSocketEncoder implements IChannelEncoder {
    private final IServerSession session;


    /**
     * Constructor.
     * 
     * @param session the {@link IServerSession}
     */
    public WebSocketEncoder(final IServerSession session) {
        this.session = session;
    }


    @Override
    public Pair<Status, Request> decode(final ByteBuffer buffer) throws IOException {
        final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_SSL_ENGINE, SSLEngine.class);
        final ByteBuffer unwrappedBuffer = SslSupport.unwrap(sslEngine, session.getChannel(), buffer);
        return new Pair<>(Status.FULLY_READ, new Request(unwrappedBuffer));
    }


    @Override
    public ByteBuffer[] encode(final ByteBuffer... buffers) throws IOException {
        final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_SSL_ENGINE, SSLEngine.class);

        return SslSupport.wrap(sslEngine, session.getChannel(), buffers);
    }


    @Override
    public void close() throws Exception {
    }

}
