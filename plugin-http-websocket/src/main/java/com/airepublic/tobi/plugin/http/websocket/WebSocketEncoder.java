package com.airepublic.tobi.plugin.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.inject.Inject;
import javax.net.ssl.SSLEngine;

import com.airepublic.http.common.SslSupport;
import com.airepublic.tobi.core.spi.IChannelEncoder;
import com.airepublic.tobi.core.spi.IRequest;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Pair;
import com.airepublic.tobi.module.http.HttpRequest;
import com.airepublic.tobi.module.http.SessionConstants;

/**
 * The {@link IChannelEncoder} implementation for the websocket requests/responses supporting SSL.
 * 
 * @author Torsten Oltmanns
 *
 */
public class WebSocketEncoder implements IChannelEncoder {
    @Inject
    private IServerSession session;


    @Override
    public Pair<Status, IRequest> decode(final ByteBuffer buffer) throws IOException {
        final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_SSL_ENGINE, SSLEngine.class);
        final ByteBuffer unwrappedBuffer = SslSupport.unwrap(sslEngine, session.getChannel(), buffer);
        return new Pair<>(Status.FULLY_READ, new HttpRequest(session, null, null, unwrappedBuffer));
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
