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


    /**
     * Called during initialization of this IO handler to initialize the specific implementations.
     * 
     * @throws IOException if the deployment fails
     */
    protected abstract void deploy() throws IOException;


    /**
     * Gets the {@link ServerSession}.
     * 
     * @return the {@link ServerSession}
     */
    public synchronized ServerSession getSession() {
        return session;
    }


    /**
     * Called whenever there is data available from the incoming stream.
     * 
     * @param buffer the {@link ByteBuffer} read from the incoming stream
     * @return the {@link ChannelAction} that should be performed after consuming this
     *         {@link ByteBuffer}
     * @throws IOException if an exception occurs during processing the buffer
     */
    protected abstract ChannelAction consume(ByteBuffer buffer) throws IOException;


    /**
     * Called when the outgoing stream is ready to write data.<br/>
     * NOTE: outgoing {@link ByteBuffer}s can be queue to the sessions write-buffer-queue.
     * 
     * @throws IOException if producing data fails
     */
    protected abstract void produce() throws IOException;


    /**
     * This method is called by the {@link ServerSession} if there is an exception while reading
     * from the incoming stream. In this case {@link AbstractIOHandler#consume(ByteBuffer)} will not
     * be called.
     * 
     * @param t the exception that occurred
     * @return the {@link ChannelAction} that should be performed
     */
    protected abstract ChannelAction onReadError(Throwable t);


    /**
     * Called when a {@link ChannelAction#CLOSE_INPUT} has been processed.
     * 
     * @throws IOException
     */
    protected abstract void handleClosedInput() throws IOException;


    /**
     * Called when writing {@link ByteBuffer}s to the outgoing stream was successful.
     * 
     * @param handler the {@link CompletionHandler} that should be invoked with
     *        {@link CompletionHandler#completed(Object, Object)}
     * @param length the amount of bytes written to the outgoing stream
     * @return the {@link ChannelAction} that should be performed
     */
    protected abstract ChannelAction writeSuccessful(CompletionHandler<?, ?> handler, long length);


    /**
     * Called when writing {@link ByteBuffer}s to the outgoing stream failed.
     * 
     * @param handler the {@link CompletionHandler} that should be invoked with
     *        {@link CompletionHandler#failed(Throwable, Object)}
     * @return the {@link ChannelAction} that should be performed
     */
    protected abstract ChannelAction writeFailed(CompletionHandler<?, ?> handler, Throwable t);

}
