package com.airepublic.microprofile.core.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

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
     * Adds a {@link IServicePlugin} which must match the protocol of this module.
     * 
     * @param featurePlugin the {@link IServicePlugin}
     */
    void addServicePlugin(IServicePlugin featurePlugin);


    /**
     * Gets the registered {@link IServicePlugin}s.
     */
    Set<IServicePlugin> getServicePlugins();


    /**
     * Callback method to perform initial tasks once a new connection has been accepted by the
     * server, e.g. create a SSLEngine and handshake.
     * 
     * @param session the {@link IServerSession}
     * @throws IOException if something goes wrong
     */
    void onAccept(IServerSession session) throws IOException;


    /**
     * Callback to unwrap a freshly read {@link ByteBuffer}.
     * 
     * @param session the {@link IServerSession}
     * @param buffer the {@link ByteBuffer}
     * @return the unwrapped or same {@link ByteBuffer} if unwrapping is not needed
     * @throws IOException if something goes wrong
     */
    ByteBuffer unwrap(IServerSession session, ByteBuffer buffer) throws IOException;


    /**
     * Callback to wrap {@link ByteBuffer}s ready for writing.
     * 
     * @param session the {@link IServerSession}
     * @param buffers the {@link ByteBuffer}s
     * @return the wrapped or same {@link ByteBuffer}s if wrapping is not needed
     * @throws IOException if something goes wrong
     */
    ByteBuffer[] wrap(IServerSession session, ByteBuffer... buffers) throws IOException;


    /**
     * Tries to determine if the module can handle the initial (unwrapped) {@link ByteBuffer} and
     * map it to an {@link IIOHandler}.
     * 
     * @param buffer the initial {@link ByteBuffer}
     * @param session the {@link IServerSession}
     * @return the {@link Pair} of DetermineStatus representing whether it could map a hander or not
     *         or might need more data in the buffer to determine the {@link IIOHandler}
     * @throws IOException if something goes wrong
     */
    Pair<DetermineStatus, IIOHandler> determineIoHandler(final ByteBuffer buffer, final IServerSession session) throws IOException;


    /**
     * Gets the buffer-size for the read buffer.
     * 
     * @return the buffer size
     */
    int getReadBufferSize();

}
