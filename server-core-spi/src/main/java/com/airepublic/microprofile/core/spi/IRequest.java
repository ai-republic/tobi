package com.airepublic.microprofile.core.spi;

import java.nio.ByteBuffer;
import java.util.Map;

public interface IRequest {
    /**
     * Gets any attributes, e.g. headers, etc. associated with the payload.
     * 
     * @return the attributes
     */
    Map<?, ?> getAttributes();


    /**
     * Gets the requests payload.
     * 
     * @return the {@link ByteBuffer} containing the payload
     */
    ByteBuffer getPayload();

}
