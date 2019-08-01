package com.airepublic.microprofile.core.spi;

import java.util.Set;

/**
 * Interface defining a service-plugin that is associated with a {@link IServerModule} based on its
 * supported protocols.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IServicePlugin extends AutoCloseable {
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
     * Gets the priority by which the {@link IServicePlugin} is queries when trying to determine the
     * {@link IIOHandler}.
     * 
     * @return the priority
     */
    int getPriority();


    /**
     * Tries to determine if this plugin provides a {@link IIOHandler} which can handle the
     * {@link Request}.
     *
     * @param request the {@link Request}
     * @return the {@link IIOHandler}
     */
    IIOHandler determineIoHandler(Request request);


    /**
     * Initializes the plugin for the associated {@link IServerModule}.
     * 
     * @param module the {@link IServerModule}
     */
    void initPlugin(IServerModule module);

}
