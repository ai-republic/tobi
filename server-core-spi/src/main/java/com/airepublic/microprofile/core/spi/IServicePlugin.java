package com.airepublic.microprofile.core.spi;

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
     * map it to an {@link IIOHandler}.
     * 
     * @param buffer the initial {@link ByteBuffer}
     * @param sessionAttributes the {@link SessionAttributes}
     * @return the {@link Pair} of DetermineStatus representing whether it could map a hander or not
     *         or might need more data in the buffer to determine the {@link IIOHandler}
     * @throws IOException if something goes wrong
     */
    Pair<DetermineStatus, IIOHandler> determineIoHandler(final ByteBuffer buffer, final SessionAttributes sessionAttributes) throws IOException;


    /**
     * Initializes the plugin for the associated {@link IServerModule}.
     * 
     * @param module the {@link IServerModule}
     */
    void initPlugin(IServerModule module);


    /**
     * Called when an {@link IServerSession} is created to add plugin specific attributes to the
     * session.
     * 
     * @param session the {@link IServerSession}
     */
    void onSessionCreate(IServerSession session);

}
