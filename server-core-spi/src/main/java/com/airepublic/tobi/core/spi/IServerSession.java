package com.airepublic.tobi.core.spi;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;

import javax.enterprise.context.SessionScoped;

@SessionScoped
public interface IServerSession extends Serializable, AutoCloseable, SessionConstants {

    String getId();


    void setId(String id);


    IChannelProcessor getChannelProcessor();


    void setChannelProcessor(IChannelProcessor channelProcessor);


    SocketChannel getChannel();


    void addToReadBuffer(ByteBuffer... buffer);


    void addToWriteBuffer(ByteBuffer... buffer);


    void addToWriteBuffer(CompletionHandler<?, ?> handler, ByteBuffer... buffer);


    @Override
    void close();


    void setAttribute(String key, Object value);


    <T> T getAttribute(String key, Class<T> type);


    Attributes getSessionAttributes();

}