package com.airepublic.microprofile.core;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;

import com.airepublic.microprofile.core.spi.IChannelProcessor;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.Attributes;

public class ServerSession implements IServerSession {
    private static final long serialVersionUID = 1L;
    private static AtomicLong SESSION_ID_GENERATOR = new AtomicLong();
    private final String id = "" + SESSION_ID_GENERATOR.incrementAndGet();
    private final Attributes attributes = new Attributes();
    private transient IChannelProcessor channelProcessor;


    @Override
    public String getId() {
        return id;
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


    @Override
    public void setAttribute(final String key, final Object value) {
        attributes.set(key, value);
    }


    @Override
    public <T> T getAttribute(final String key, final Class<T> type) {
        return attributes.get(key, type);
    }


    @Override
    public Attributes getSessionAttributes() {
        return attributes;
    }
}
