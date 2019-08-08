package com.airepublic.tobi.core.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public interface IChannelProcessor extends Runnable, AutoCloseable {

    void prepare(final IServerModule module, final SocketChannel channel, final IIOHandler ioHandler) throws IOException;


    @Override
    void run();


    IServerSession getSession();


    IChannelEncoder getChannelEncoder();


    void setChannelEncoder(IChannelEncoder channelEncoder);


    void addToReadBuffer(ByteBuffer... buffer);


    void addToWriteBuffer(ByteBuffer... buffer);


    void addToWriteBuffer(CompletionHandler<?, ?> handler, ByteBuffer... buffer);


    @Override
    void close();


    IServerModule getModule();


    Selector getSelector();


    SocketChannel getChannel();


    IIOHandler getIoHandler();

}