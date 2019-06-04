package com.airepublic.microprofile.core;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.microprofile.config.Config;

/**
 * Service provider interface to provide information about a server module.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IServerModule {
    /**
     * The name of the server module.
     * 
     * @return the name
     */
    String getName();


    /**
     * Initializes the server module from the specified {@link Config} and adds any context
     * information it needs to the {@link ServerContext}.
     * 
     * @param config the configuration
     * @param serverContext the {@link ServerContext}
     * @throws IOException if the initialization fails
     */
    void initModule(Config config, ServerContext serverContext) throws IOException;


    /**
     * Tries to determine if the module can handle the initial buffer and map it to an
     * {@link AbstractIOHandler}.
     * 
     * 
     * @return the {@link Pair} of DetermineStatus representing whether it could map a hander or not
     *         or might need more data in the buffer to determine the {@link AbstractIOHandler}
     */
    Pair<DetermineStatus, AbstractIOHandler> determineHandlerClass(final ByteBuffer buffer, final ServerSession session) throws IOException;

}
