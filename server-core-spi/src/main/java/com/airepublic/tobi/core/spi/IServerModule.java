package com.airepublic.tobi.core.spi;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Service provider interface to provide information about a server module.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IServerModule extends Closeable {
    /**
     * The name of the server module.
     * 
     * @return the name
     */
    String getName();


    /**
     * Gets the ports to open for the module, e.g. 80 for HTTP and 443 for HTTPS.
     * 
     * @return the list of ports that should be opened in the server
     */
    int[] getPortsToOpen();


    /**
     * Gets the provided protocol of this module, i.e. HTTP for HTTP, HTTPS and WebSockets
     * 
     * @return the protocol
     */
    String getProtocol();


    /**
     * Checks whether the {@link IServerSession} or {@link IRequest} is authorized.
     * 
     * @param session the {@link IServerSession}
     * @throws SecurityException if the request is not authorized
     * @throws IOException if the request is invalid
     */
    void checkAuthorization(IServerSession session) throws SecurityException, IOException;


    /**
     * Adds a {@link IServicePlugin} which must match the protocol of this module.
     * 
     * @param featurePlugin the {@link IServicePlugin}
     */
    void addServicePlugin(IServicePlugin featurePlugin);


    /**
     * Gets the registered {@link IServicePlugin}s.
     * 
     * @return the set of registered {@link IServicePlugin}s
     */
    Set<IServicePlugin> getServicePlugins();


    /**
     * Called if when an incoming request was made to the server port and the module should accept
     * the {@link SocketChannel} and process the request.
     * 
     * @param processor the {@link IChannelProcessor}
     * @throws IOException if the channel cannot be accepted
     */
    void accept(final IChannelProcessor processor) throws IOException;


    /**
     * Tries to determine which registered {@link IIOHandler} can handle the {@link IRequest}.
     *
     * @param request the {@link IRequest}
     * @return the {@link IIOHandler}
     * @throws IOException if something goes wrong
     */
    IIOHandler determineIoHandler(IRequest request) throws IOException;


    /**
     * Gets the buffer-size for the read buffer.
     * 
     * @return the buffer size
     */
    int getReadBufferSize();

}
