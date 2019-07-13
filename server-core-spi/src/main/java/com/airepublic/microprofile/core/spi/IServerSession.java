package com.airepublic.microprofile.core.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface IServerSession {
    String SESSION_IS_SECURE = "session.isSecure";
    String SESSION_IO_HANDLER = "session.ioHandler";
    String SESSION_IO_HANDLER_CLASS = "session.ioHandler.class";


    String getId();


    SelectionKey getSelectionKey();


    SocketChannel getChannel();


    boolean isSecure();


    void setSecure(boolean isSecure);


    void setAttribute(String key, Object value);


    <T> T getAttribute(String key, Class<T> type);


    void open(IServerModule module, SocketChannel channel, SessionAttributes sessionAttributes, boolean isClient) throws IOException;


    void close();


    boolean isClosed();


    void addToReadBuffer(ByteBuffer... buffer);


    ByteBuffer getNextReadBuffer();


    void addToWriteBuffer(ByteBuffer... buffer);


    void addToWriteBuffer(CompletionHandler<?, ?> handler, ByteBuffer... buffer);


    Pair<ByteBuffer[], CompletionHandler<?, ?>> getNextWriteBuffer();

}