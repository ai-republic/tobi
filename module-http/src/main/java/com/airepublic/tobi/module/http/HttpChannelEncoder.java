package com.airepublic.tobi.module.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.airepublic.http.common.AsyncHttpReader;
import com.airepublic.http.common.SslSupport;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.Attributes;
import com.airepublic.tobi.core.spi.IChannelEncoder;
import com.airepublic.tobi.core.spi.IRequest;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Pair;

/**
 * Encodes/Decodes {@link ByteBuffer} of incoming/outgoing HTTP responses/requests. This includes
 * SSL processing.
 * 
 * @author Torsten Oltmanns
 *
 */
public class HttpChannelEncoder implements AutoCloseable, IChannelEncoder {
    public static final String HEADERS = "http.headers";
    public static final String REQUEST_LINE = "http.request.requestLine";

    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private SSLEngine sslEngine;
    private boolean isSecure;
    private IServerSession session;
    private final AsyncHttpReader httpReader = new AsyncHttpReader();

    /**
     * Initializes this encoder with the specified {@link IServerSession} and {@link SSLContext}.
     * 
     * @param session the {@link IServerSession}
     * @param sslContext the {@link SSLContext}
     * @param isSecure whether the connection is secure
     * @throws IOException if SSL handshaking fails
     */
    public void init(final IServerSession session, final SSLContext sslContext, final boolean isSecure) throws IOException {
        this.session = session;
        this.isSecure = isSecure;

        // create SSL engine if necessary
        if (isSecure) {
            sslEngine = SslSupport.serverSSLHandshake(sslContext, session.getChannel());
        }
    }


    /**
     * Gets the {@link SSLEngine}.
     * 
     * @return the {@link SSLEngine}
     */
    public SSLEngine getSslEngine() {
        return sslEngine;
    }


    @Override
    public Pair<Status, IRequest> decode(ByteBuffer buffer) throws IOException {
        if (isSecure) {
            buffer = SslSupport.unwrap(sslEngine, session.getChannel(), buffer);
        }

        if (buffer == null) {
            return new Pair<>(Status.CLOSED, null);
        }

        if (httpReader.receiveBuffer(buffer)) {
            final com.airepublic.http.common.HttpRequest request = httpReader.getHttpRequest();
            httpReader.clear();

            logger.log(Level.INFO, "Processing session #" + session.getId() + " HTTP request: " + request.getRequestLine());

            final Attributes attributes = new Attributes();
            attributes.setAttribute(REQUEST_LINE, request.getRequestLine());
            attributes.setAttribute(HEADERS, request.getHeaders());
            final IRequest wrapped = new HttpRequest(session, request.getRequestLine(), request.getHeaders(), request.getBody());
            return new Pair<>(Status.FULLY_READ, wrapped);
        }

        return new Pair<>(Status.NEED_MORE_DATA, null);
    }


    @Override
    public ByteBuffer[] encode(ByteBuffer... buffers) throws IOException {
        if (isSecure) {
            buffers = SslSupport.wrap(sslEngine, session.getChannel(), buffers);
        }

        return buffers;
    }


    @Override
    public void close() {
        if (sslEngine != null) {
            try {
                SslSupport.closeConnection(session.getChannel(), sslEngine);
            } catch (final Exception e) {
            }
        }
    }
}
