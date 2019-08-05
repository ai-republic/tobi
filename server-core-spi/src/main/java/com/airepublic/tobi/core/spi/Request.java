package com.airepublic.tobi.core.spi;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * A plain request object that provides access to {@link Attributes} of a request and its payload
 * {@link ByteBuffer}.
 * 
 * @author Torsten Oltmanns
 *
 */
public final class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private Attributes attributes;
    private ByteBuffer payload;


    /**
     * Constructor.
     */
    public Request() {
        attributes = new Attributes();
    }


    /**
     * Constructor.
     * 
     * @param attributes the {@link Attributes} associated with this request.
     * @param payload the payload
     */
    public Request(final Attributes attributes, final ByteBuffer payload) {
        this.attributes = attributes;
        this.payload = payload;
    }


    /**
     * Constructor.
     * 
     * @param payload the payload
     */
    public Request(final ByteBuffer payload) {
        this.payload = payload;
    }


    /**
     * Gets any {@link Attributes}, e.g. headers, etc. associated with the payload.
     * 
     * @return the {@link Attributes}
     */
    public Attributes getAttributes() {
        return attributes;
    }


    /**
     * Sets any {@link Attributes}, e.g. headers, etc. associated with the payload.
     * 
     * @param attributes the {@link Attributes}
     */
    public void setAttributes(final Attributes attributes) {
        this.attributes = attributes;
    }


    /**
     * Gets the requests payload.
     * 
     * @return the {@link ByteBuffer} containing the payload
     */
    public ByteBuffer getPayload() {
        return payload;
    }


    /**
     * Sets the requests payload.
     * 
     * @param payload the {@link ByteBuffer} containing the payload
     */
    public void getPayload(final ByteBuffer payload) {
        this.payload = payload;
    }

}
