package com.airepublic.tobi.core.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * The interface to provide the central functionality to process the {@link SocketChannel} IO.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IChannelProcessor extends Runnable, AutoCloseable {

    /**
     * Prepare the {@link IChannelProcessor} to be reused for a new connection.
     * 
     * @param module the {@link IServerModule}
     * @param channel the {@link SocketChannel}
     * @param ioHandler the {@link IIOHandler} (optional)
     * @throws IOException if something goes wrong
     */
    void prepare(final IServerModule module, final SocketChannel channel, final IIOHandler ioHandler) throws IOException;


    @Override
    void run();


    /**
     * Gets the associated {@link IServerSession}.
     * 
     * @return the {@link IServerSession}
     */
    IServerSession getSession();


    /**
     * Gets the associated {@link IChannelEncoder}.
     * 
     * @return the {@link IChannelEncoder}
     */
    IChannelEncoder getChannelEncoder();


    /**
     * Sets the associated {@link IChannelEncoder}.
     * 
     * @param channelEncoder the {@link IChannelEncoder}
     */
    void setChannelEncoder(IChannelEncoder channelEncoder);


    /**
     * Add the {@link ByteBuffer}s to the read queue.
     * 
     * @param buffer the {@link ByteBuffer}s
     */
    void addToReadBuffer(ByteBuffer... buffer);


    /**
     * Add the {@link ByteBuffer}s to the write queue.
     * 
     * @param buffer the {@link ByteBuffer}s
     */
    void addToWriteBuffer(ByteBuffer... buffer);


    /**
     * Add the {@link ByteBuffer}s to the write queue and use the specified
     * {@link CompletionHandler} to notify any interested.
     * 
     * @param handler the {@link CompletionHandler}
     * @param buffer the {@link ByteBuffer}s
     */
    void addToWriteBuffer(CompletionHandler<?, ?> handler, ByteBuffer... buffer);


    @Override
    void close();


    /**
     * Gets the associated {@link IServerModule}.
     * 
     * @return the {@link IServerModule}
     */
    IServerModule getModule();


    /**
     * Gets the {@link Selector} used to listen on the {@link SocketChannel}.
     * 
     * @return the {@link Selector}
     */
    Selector getSelector();


    /**
     * Gets the associated {@link SocketChannel}.
     * 
     * @return the {@link SocketChannel}
     */
    SocketChannel getChannel();


    /**
     * Gets the associated {@link IIOHandler} to consumer and produce IO.
     * 
     * @return the {@link IIOHandler}
     */
    IIOHandler getIoHandler();

}