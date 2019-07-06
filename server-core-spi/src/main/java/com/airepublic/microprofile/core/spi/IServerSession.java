package com.airepublic.microprofile.core.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

public interface IServerSession {
    String SESSION_IS_SECURE = "session.issecure";
    String SESSION_IO_HANDLER = "session.iohandler";
    String SESSION_IO_HANDLER_CLASS = "session.iohandler.class";


    long getId();


    SelectionKey getSelectionKey();


    SocketChannel getChannel();


    boolean isClosed();


    void setAttribute(String key, Object value);


    Object getAttribute(String key);


    void open(long id, IServerModule module, SocketChannel channel, Map<String, Object> attributes, boolean isOutbound) throws IOException;


    void close();


    void addToReadBuffer(ByteBuffer... buffer);


    ByteBuffer getNextReadBuffer();


    void addToWriteBuffer(ByteBuffer... buffer);


    void addToWriteBuffer(CompletionHandler<?, ?> handler, ByteBuffer... buffer);


    Pair<ByteBuffer[], CompletionHandler<?, ?>> getNextWriteBuffer();

}