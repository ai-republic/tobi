package com.airepublic.tobi.core;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;

import javax.enterprise.context.SessionScoped;

import com.airepublic.tobi.core.spi.Attributes;
import com.airepublic.tobi.core.spi.IChannelProcessor;
import com.airepublic.tobi.core.spi.IServerSession;

/**
 * The server session which can store session relevant information.
 * 
 * @author Torsten Oltmanns
 *
 */
@SessionScoped
public class ServerSession extends Attributes implements IServerSession {
    private static final long serialVersionUID = 1L;
    private String id;
    private transient IChannelProcessor channelProcessor;


    @Override
    public String getId() {
        return id;
    }


    @Override
    public void setId(final String id) {
        this.id = id;
    }


    @Override
    public IChannelProcessor getChannelProcessor() {
        return channelProcessor;
    }


    @Override
    public void setChannelProcessor(final IChannelProcessor channelProcessor) {
        this.channelProcessor = channelProcessor;
    }


    @Override
    public SocketChannel getChannel() {
        return getChannelProcessor() != null ? getChannelProcessor().getChannel() : null;
    }


    @Override
    public void addToReadBuffer(final ByteBuffer... buffer) {
        if (getChannelProcessor() != null) {
            getChannelProcessor().addToReadBuffer(buffer);
        }
    }


    @Override
    public void addToWriteBuffer(final ByteBuffer... buffers) {
        if (getChannelProcessor() != null) {
            getChannelProcessor().addToWriteBuffer(buffers);
        }
    }


    @Override
    public void addToWriteBuffer(final CompletionHandler<?, ?> handler, final ByteBuffer... buffers) {
        if (getChannelProcessor() != null) {
            getChannelProcessor().addToWriteBuffer(handler, buffers);
        }
    }


    @Override
    public void close() {
        if (getChannelProcessor() != null) {
            getChannelProcessor().close();
        }
    }

}
