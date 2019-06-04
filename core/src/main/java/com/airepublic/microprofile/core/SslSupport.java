package com.airepublic.microprofile.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslSupport {
    private final static Logger LOG = LoggerFactory.getLogger(SslSupport.class);
    private final static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static int applicationBufferSize = 16 * 1024;
    private static int packetBufferSize = 16 * 1024;


    /**
     * Creates the key managers required to initiate the {@link SSLContext}, using a JKS keystore as
     * an input.
     *
     * @param filepath - the path to the JKS keystore.
     * @param keystorePassword - the keystore's password.
     * @param keyPassword - the key's passsword.
     * @return {@link KeyManager} array that will be used to initiate the {@link SSLContext}.
     * @throws Exception
     */
    static KeyManager[] createKeyManagers(final String filepath, final String keystorePassword, final String keyPassword) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        final InputStream keyStoreIS = new FileInputStream(filepath);

        try {
            keyStore.load(keyStoreIS, keystorePassword.toCharArray());
        } finally {
            if (keyStoreIS != null) {
                keyStoreIS.close();
            }
        }

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPassword.toCharArray());
        return kmf.getKeyManagers();
    }


    /**
     * Creates the trust managers required to initiate the {@link SSLContext}, using a JKS keystore
     * as an input.
     *
     * @param filepath - the path to the JKS keystore.
     * @param keystorePassword - the keystore's password.
     * @return {@link TrustManager} array, that will be used to initiate the {@link SSLContext}.
     * @throws Exception
     */
    static TrustManager[] createTrustManagers(final String filepath, final String keystorePassword) throws Exception {
        final KeyStore trustStore = KeyStore.getInstance("JKS");
        final InputStream trustStoreIS = new FileInputStream(filepath);

        try {
            trustStore.load(trustStoreIS, keystorePassword.toCharArray());
        } finally {
            if (trustStoreIS != null) {
                trustStoreIS.close();
            }
        }

        final TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(trustStore);
        return trustFactory.getTrustManagers();
    }


    static ByteBuffer enlargeApplicationBuffer(final SSLEngine engine, final ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
    }


    /**
     * Implements the handshake protocol between two peers, required for the establishment of the
     * SSL/TLS connection. During the handshake, encryption configuration information - such as the
     * list of available cipher suites - will be exchanged and if the handshake is successful will
     * lead to an established SSL/TLS session.
     *
     * <p/>
     * A typical handshake will usually contain the following steps:
     *
     * <ul>
     * <li>1. wrap: ClientHello</li>
     * <li>2. unwrap: ServerHello/Cert/ServerHelloDone</li>
     * <li>3. wrap: ClientKeyExchange</li>
     * <li>4. wrap: ChangeCipherSpec</li>
     * <li>5. wrap: Finished</li>
     * <li>6. unwrap: ChangeCipherSpec</li>
     * <li>7. unwrap: Finished</li>
     * </ul>
     * <p/>
     * Handshake is also used during the end of the session, in order to properly close the
     * connection between the two peers. A proper connection close will typically include the one
     * peer sending a CLOSE message to another, and then wait for the other's CLOSE message to close
     * the transport link. The other peer from his perspective would read a CLOSE message from his
     * peer and then enter the handshake procedure to send his own CLOSE message as well.
     *
     * @param socketChannel - the socket channel that connects the two peers.
     * @param engine - the engine that will be used for encryption/decryption of the data exchanged
     *        with the other peer.
     * @return True if the connection handshake was successful or false if an error occurred.
     * @throws IOException - if an error occurs during read/write to the socket channel.
     */
    static boolean doHandshake(final SocketChannel socketChannel, final SSLEngine engine) throws IOException {

        LOG.debug("About to do handshake...");
        ByteBuffer packetBuffer = ByteBuffer.allocate(packetBufferSize);
        ByteBuffer peerPacketBuffer = ByteBuffer.allocate(packetBufferSize);

        SSLEngineResult result;
        HandshakeStatus handshakeStatus;

        // NioSslPeer's fields myAppData and peerAppData are supposed to be large enough to hold all
        // message data the peer
        // will send and expects to receive from the other peer respectively. Since the messages to
        // be exchanged will usually be less
        // than 16KB long the capacity of these fields should also be smaller. Here we initialize
        // these two local buffers
        // to be used for the handshake, while keeping client's buffers at the same size.
        final int appBufferSize = engine.getSession().getApplicationBufferSize();
        final ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
        packetBuffer.clear();
        peerPacketBuffer.clear();

        handshakeStatus = engine.getHandshakeStatus();
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    if (socketChannel.read(peerPacketBuffer) < 0) {
                        if (engine.isInboundDone() && engine.isOutboundDone()) {
                            return false;
                        }
                        try {
                            engine.closeInbound();
                        } catch (final SSLException e) {
                            LOG.error("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.", e);
                        }
                        engine.closeOutbound();
                        // After closeOutbound the engine will be set to WRAP state, in order to try
                        // to send a close message to the client.
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    peerPacketBuffer.flip();
                    try {
                        result = engine.unwrap(peerPacketBuffer, peerAppData);
                        peerPacketBuffer.compact();
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (final SSLException e) {
                        LOG.error("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...", e);
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK:
                        break;
                        case BUFFER_OVERFLOW:
                            // Will occur when peerAppData's capacity is smaller than the data
                            // derived from peerNetData's unwrap.
                            peerAppData = SslSupport.enlargeApplicationBuffer(engine, peerAppData);
                        break;
                        case BUFFER_UNDERFLOW:
                            // Will occur either when no data was read from the peer or when the
                            // peerNetData buffer was too small to hold all peer's data.
                            peerPacketBuffer = SslSupport.handleBufferUnderflow(engine, peerPacketBuffer);
                        break;
                        case CLOSED:
                            if (engine.isOutboundDone()) {
                                return false;
                            } else {
                                engine.closeOutbound();
                                handshakeStatus = engine.getHandshakeStatus();
                                break;
                            }
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                break;
                case NEED_WRAP:
                    packetBuffer.clear();
                    try {
                        result = engine.wrap(myAppData, packetBuffer);
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (final SSLException sslException) {
                        LOG.error("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK:
                            packetBuffer.flip();
                            while (packetBuffer.hasRemaining()) {
                                socketChannel.write(packetBuffer);
                            }
                        break;
                        case BUFFER_OVERFLOW:
                            // Will occur if there is not enough space in myNetData buffer to write
                            // all the data that would be generated by the method wrap.
                            // Since myNetData is set to session's packet size we should not get to
                            // this point because SSLEngine is supposed
                            // to produce messages smaller or equal to that, but a general handling
                            // would be the following:
                            packetBuffer = SslSupport.enlargePacketBuffer(engine, packetBuffer);
                        break;
                        case BUFFER_UNDERFLOW:
                            throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                        case CLOSED:
                            try {
                                packetBuffer.flip();
                                while (packetBuffer.hasRemaining()) {
                                    socketChannel.write(packetBuffer);
                                }
                                // At this point the handshake status will probably be NEED_UNWRAP
                                // so we make sure that peerNetData is clear to read.
                                peerPacketBuffer.clear();
                            } catch (final Exception e) {
                                LOG.error("Failed to send server's CLOSE message due to socket channel's failure.");
                                handshakeStatus = engine.getHandshakeStatus();
                            }
                        break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                break;
                case NEED_TASK:
                    Runnable task;

                    while ((task = engine.getDelegatedTask()) != null) {
                        executorService.execute(task);
                    }

                    handshakeStatus = engine.getHandshakeStatus();
                break;
                case FINISHED:
                break;
                case NOT_HANDSHAKING:
                break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
            }
        }

        return true;
    }


    /**
     * Compares <code>sessionProposedCapacity<code> with buffer's capacity. If buffer's capacity is
     * smaller, returns a buffer with the proposed capacity. If it's equal or larger, returns a
     * buffer with capacity twice the size of the initial one.
     *
     * @param buffer - the buffer to be enlarged.
     * @param sessionProposedCapacity - the minimum size of the new buffer, proposed by
     *        {@link SSLSession}.
     * @return A new buffer with a larger capacity.
     */
    static ByteBuffer enlargeBuffer(ByteBuffer buffer, final int sessionProposedCapacity) {
        if (sessionProposedCapacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(sessionProposedCapacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }

        return buffer;
    }


    static ByteBuffer enlargePacketBuffer(final SSLEngine engine, final ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
    }


    /**
     * Handles {@link SSLEngineResult.Status#BUFFER_UNDERFLOW}. Will check if the buffer is already
     * filled, and if there is no space problem will return the same buffer, so the client tries to
     * read again. If the buffer is already filled will try to enlarge the buffer either to
     * session's proposed size or to a larger capacity. A buffer underflow can happen only after an
     * unwrap, so the buffer will always be a peerNetData buffer.
     *
     * @param buffer - will always be peerNetData buffer.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the
     *        two peers.
     * @return The same buffer if there is no space problem or a new buffer with the same data but
     *         more space.
     * @throws Exception
     */
    static ByteBuffer handleBufferUnderflow(final SSLEngine engine, final ByteBuffer buffer) {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
            return buffer;
        } else {
            final ByteBuffer replaceBuffer = enlargePacketBuffer(engine, buffer);
            buffer.flip();
            replaceBuffer.put(buffer);
            return replaceBuffer;
        }
    }


    /**
     * This method should be called when this peer wants to explicitly close the connection or when
     * a close message has arrived from the other peer, in order to provide an orderly shutdown.
     * <p/>
     * It first calls {@link SSLEngine#closeOutbound()} which prepares this peer to send its own
     * close message and sets {@link SSLEngine} to the <code>NEED_WRAP</code> state. Then, it
     * delegates the exchange of close messages to the handshake method and finally, it closes
     * socket channel.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the
     *        two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    static void closeConnection(final SocketChannel socketChannel, final SSLEngine engine) throws IOException {
        engine.closeOutbound();
        doHandshake(socketChannel, engine);
        socketChannel.close();
    }


    static void setPacketBufferSize(final int size) {
        packetBufferSize = size;
    }


    static int getPacketBufferSize() {
        return packetBufferSize;
    }


    static void setApplicationBufferSize(final int size) {
        applicationBufferSize = size;
    }


    static int getApplicationBufferSize() {
        return applicationBufferSize;
    }
}
