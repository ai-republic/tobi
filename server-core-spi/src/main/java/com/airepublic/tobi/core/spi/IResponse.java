package com.airepublic.tobi.core.spi;

import java.nio.ByteBuffer;

/**
 * A plain response object that provides access to the {@link Attributes} of the response and its
 * payload {@link ByteBuffer}.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IResponse {

    /**
     * Gets request attributes that do not belong to the payload.
     * 
     * @param <T> the value type
     * @param key the attribute key
     * @return the value
     */
    <T> T getAttribute(String key);


    /**
     * Sets request attributes that do not belong to the payload.
     * 
     * @param <T> the value type
     * @param key the attribute key
     * @param value the attribute value
     * @return the value
     */
    <T> void setAttribute(String key, T value);


    /**
     * Gets the {@link Attributes} as {@link ByteBuffer}.
     * 
     * @return the attributes as {@link ByteBuffer}
     */
    ByteBuffer getAttributesBuffer();


    /**
     * Gets the payload.
     * 
     * @return the payload {@link ByteBuffer}
     */
    ByteBuffer getPayload();
}
