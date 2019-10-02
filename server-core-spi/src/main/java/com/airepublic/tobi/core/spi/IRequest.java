package com.airepublic.tobi.core.spi;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * A plain request object that provides access to the {@link IServerSession}, {@link Attributes} of
 * a request and its payload {@link ByteBuffer}.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IRequest extends Serializable {

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
     * Gets the associated {@link IServerSession}.
     * 
     * @return the associated {@link IServerSession}
     */
    IServerSession getSession();


    /**
     * Gets the requests payload.
     * 
     * @return the {@link ByteBuffer} containing the payload
     */
    ByteBuffer getPayload();


    /**
     * Sets the requests payload.
     * 
     * @param payload the {@link ByteBuffer} containing the payload
     */
    void setPayload(ByteBuffer payload);

}