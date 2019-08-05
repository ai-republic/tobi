package com.airepublic.tobi.core.spi;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface IChannelEncoder {

    public enum Status {
        FULLY_READ, NEED_MORE_DATA;
    }


    Pair<Status, Request> decode(ByteBuffer buffer) throws IOException;


    ByteBuffer[] encode(ByteBuffer... buffers) throws IOException;


    void close() throws Exception;

}