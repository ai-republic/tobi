package com.airepublic.microprofile.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * The IO-handler is the central part for consuming and producing data for the connection.
 * 
 * @author Torsten Oltmanns
 *
 */
public abstract class AbstractIOHandler {
    private ServerSession session;


    /**
     * Initializes this IO-handler for the specified {@link ServerSession}.
     * 
     * @param session the {@link ServerSession}
     * @throws IOException if the initialization fails
     */
    public final void init(final ServerSession session) throws IOException {
        this.session = session;

        deploy();
    }


    protected abstract void deploy() throws IOException;


    public synchronized ServerSession getSession() {
        return session;
    }


    protected abstract ChannelAction consume(ByteBuffer buffer) throws IOException;


    protected abstract void produce() throws IOException;


    protected abstract ChannelAction onReadError(Exception e);


    protected abstract void handleClosedInput() throws IOException;


    protected abstract ChannelAction writeSuccessful(CompletionHandler<?, ?> handler, long length);


    protected abstract ChannelAction writeFailed(CompletionHandler<?, ?> handler, Throwable t);

}
