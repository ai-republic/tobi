package com.airepublic.tobi.core.spi;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Implementors of this class will be called right after the {@link ByteBuffer} has been read or
 * right before it being written to/from the connection. The decoding part will also parse the
 * request. It is intended to perform de-/encryption or manipulation of the payload.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IChannelEncoder {

    /**
     * Status of the decoding.
     * 
     * @author Torsten Oltmanns
     *
     */
    public enum Status {
        /**
         * The request was completely read.
         */
        FULLY_READ,
        /**
         * The async request was not fully read yet.
         */
        NEED_MORE_DATA,
        /**
         * The connection is already closed.
         */
        CLOSED;
    }


    /**
     * Decodes the incoming {@link ByteBuffer} to a {@link IRequest} object.
     * 
     * @param buffer the {@link ByteBuffer}
     * @return a {@link Pair} of {@link Status} and {@link IRequest} giving information if the
     *         request was fully read
     * @throws IOException if decoding fails
     */
    Pair<Status, IRequest> decode(ByteBuffer buffer) throws IOException;


    /**
     * Encodes the {@link ByteBuffer}s to be written to the connection.
     * 
     * @param buffers the {@link ByteBuffer}s
     * @return the encoded {@link ByteBuffer}s
     * @throws IOException if encoding fails
     */
    ByteBuffer[] encode(ByteBuffer... buffers) throws IOException;


    /**
     * Closes the encoder and frees all resources.
     * 
     * @throws Exception if an exception occurred during closing
     */
    void close() throws Exception;

}