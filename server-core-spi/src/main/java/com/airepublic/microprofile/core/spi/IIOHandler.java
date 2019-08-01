package com.airepublic.microprofile.core.spi;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * The IO-handler is the central part for consuming and producing data for the connection.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IIOHandler extends Serializable {

    /**
     * Gets the current {@link IServerSession}.
     * 
     * @return the {@link IServerSession}
     */
    IServerSession getSession();


    /**
     * Sets the current {@link IServerSession}.
     * 
     * @param session the {@link IServerSession}
     */
    void setSession(IServerSession session);


    /**
     * Called whenever there is data available from the incoming stream.
     * 
     * @param request the {@link Request} read from the incoming stream
     * @return the {@link ChannelAction} that should be performed after consuming this
     *         {@link Request}
     * @throws IOException if an exception occurs during processing the buffer
     */
    ChannelAction consume(Request request) throws IOException;


    /**
     * Called when the outgoing stream is ready to write data.<br/>
     * NOTE: outgoing {@link ByteBuffer}s can be queued to the {@link IServerSession}
     * write-buffer-queue.
     * 
     * @param session the {@link IServerSession}
     * @throws IOException if producing data fails
     */
    void produce() throws IOException;


    /**
     * This method is called by the {@link IChannelProcessor} if there is an exception while reading
     * from the incoming stream. In this case {@link IIOHandler#consume(ByteBuffer)} will not be
     * called.
     * 
     * @param t the exception that occurred
     * @return the {@link ChannelAction} that should be performed
     */
    ChannelAction onReadError(Throwable t);


    /**
     * Called when a {@link ChannelAction#CLOSE_INPUT} has been processed.
     * 
     * @throws IOException
     */
    void handleClosedInput() throws IOException;


    /**
     * Called when writing {@link ByteBuffer}s to the outgoing stream was successful.
     * 
     * @param handler the {@link CompletionHandler} that should be invoked with
     *        {@link CompletionHandler#completed(Object, Object)}
     * @param length the amount of bytes written to the outgoing stream
     * @return the {@link ChannelAction} that should be performed
     */
    ChannelAction writeSuccessful(CompletionHandler<?, ?> handler, long length);


    /**
     * Called when writing {@link ByteBuffer}s to the outgoing stream failed.
     * 
     * @param handler the {@link CompletionHandler} that should be invoked with
     *        {@link CompletionHandler#failed(Throwable, Object)}
     * @return the {@link ChannelAction} that should be performed
     */
    ChannelAction writeFailed(CompletionHandler<?, ?> handler, Throwable t);


    /**
     * Called when session is about to be closed.
     */
    void onSessionClose();

}
