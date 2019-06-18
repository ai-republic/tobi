package com.airepublic.microprofile.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Interface defining a service-plugin that is associated with a {@link IServerModule} based on its
 * supported protocols.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IServicePlugin {
    /**
     * Gets the name of the plugin.
     * 
     * @return the name
     */
    String getName();


    /**
     * Gets the supported protocols to hook the plugin under a {@link IServerModule}.
     * 
     * @return the supported protocols
     */
    Set<String> getSupportedProtocols();


    /**
     * Tries to determine if the plugin can handle the initial (unwrapped) {@link ByteBuffer} and
     * map it to an {@link AbstractIOHandler}.
     * 
     * @param buffer the initial {@link ByteBuffer}
     * @param session the {@link ServerSession}
     * @return the {@link Pair} of DetermineStatus representing whether it could map a hander or not
     *         or might need more data in the buffer to determine the {@link AbstractIOHandler}
     * @throws IOException if something goes wrong
     */
    Pair<DetermineStatus, AbstractIOHandler> determineIoHandler(final ByteBuffer buffer, final ServerSession session) throws IOException;

}
