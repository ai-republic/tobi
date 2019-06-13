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
     * Gets the ports to open for the module, e.g. 80 for HTTP and 443 for HTTPS.
     * 
     * @return the list of ports that should be opened in the server
     */
    int[] getPortsToOpen();


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
     * Callback method to perform initial tasks once a new connection has been accepted by the
     * server, e.g. create a SSLEngine and handshake.
     * 
     * @param session the {@link ServerSession}
     * @throws IOException if something goes wrong
     */
    void onAccept(ServerSession session) throws IOException;


    /**
     * Callback to unwrap a freshly read {@link ByteBuffer}.
     * 
     * @param session the {@link ServerSession}
     * @param buffer the {@link ByteBuffer}
     * @return the unwrapped or same {@link ByteBuffer} if unwrapping is not needed
     * @throws IOException if something goes wrong
     */
    ByteBuffer unwrap(ServerSession session, ByteBuffer buffer) throws IOException;


    /**
     * Callback to wrap {@link ByteBuffer}s ready for writing.
     * 
     * @param session the {@link ServerSession}
     * @param buffers the {@link ByteBuffer}s
     * @return the wrapped or same {@link ByteBuffer}s if wrapping is not needed
     * @throws IOException if something goes wrong
     */
    ByteBuffer[] wrap(ServerSession session, ByteBuffer... buffers) throws IOException;


    /**
     * Tries to determine if the module can handle the initial (unwrapped) {@link ByteBuffer} and
     * map it to an {@link AbstractIOHandler}.
     * 
     * @param buffer the initial {@link ByteBuffer}
     * @param session the {@link ServerSession}
     * @return the {@link Pair} of DetermineStatus representing whether it could map a hander or not
     *         or might need more data in the buffer to determine the {@link AbstractIOHandler}
     * @throws IOException if something goes wrong
     */
    Pair<DetermineStatus, AbstractIOHandler> determineIoHandler(final ByteBuffer buffer, final ServerSession session) throws IOException;


    /**
     * Gets the buffer-size for the read buffer.
     * 
     * @return the buffer size
     */
    int getReadBufferSize();

}
