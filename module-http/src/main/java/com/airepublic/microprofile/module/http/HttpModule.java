package com.airepublic.microprofile.module.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.airepublic.microprofile.core.spi.DetermineStatus;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.core.spi.Pair;
import com.airepublic.microprofile.core.spi.SessionAttributes;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.util.http.common.BufferUtil;
import com.airepublic.microprofile.util.http.common.IServicePluginHttp;
import com.airepublic.microprofile.util.http.common.SessionConstants;

@ApplicationScoped
public class HttpModule implements IServerModule {
    public final static String PORT = "http.port";
    public final static String SSL_PORT = "http.ssl.port";
    public final static String KEYSTORE_FILE = "http.keystore.file";
    public final static String KEYSTORE_PASSWORD = "http.keystore.password";
    public final static String TRUSTSTORE_FILE = "http.truststore.file";
    public final static String TRUSTSTORE_PASSWORD = "http.truststore.password";
    @Inject
    @LoggerConfig(level = LogLevel.FINE)
    private Logger logger;
    private final Set<IServicePlugin> servicePlugins = new HashSet<>();
    @Inject
    @ConfigProperty(name = PORT)
    private Integer port;
    @Inject
    @ConfigProperty(name = SSL_PORT)
    private Integer sslPort;
    @Inject
    @ConfigProperty(name = KEYSTORE_FILE, defaultValue = "D:/keystore.jks")
    private String keystoreFile;
    @Inject
    @ConfigProperty(name = KEYSTORE_PASSWORD, defaultValue = "changeit")
    private String keystorePassword;
    @Inject
    @ConfigProperty(name = TRUSTSTORE_FILE, defaultValue = "D:/cacerts.jks")
    private String truststoreFile;
    @Inject
    @ConfigProperty(name = TRUSTSTORE_PASSWORD, defaultValue = "changeit")
    private String truststorePassword;
    @Inject
    private IServerContext serverContext;
    private SSLContext sslContext;
    private int readBufferSize = 16 * 1024;


    protected IServerContext getServerContext() {
        return serverContext;
    }


    @Override
    public String getName() {
        return "HTTP";
    }


    @Override
    public int[] getPortsToOpen() {
        if (port != null && sslPort != null) {
            return new int[] { port, sslPort };
        } else if (port != null) {
            return new int[] { port };
        } else if (sslPort != null) {
            return new int[] { sslPort };
        }

        return null;
    }


    @Override
    public String getProtocol() {
        return "HTTP";
    }


    @Override
    public void addServicePlugin(final IServicePlugin servicePlugin) {
        if (servicePlugin != null) {
            if (!servicePlugins.contains(servicePlugin)) {
                logger.info("\tAdding service-plugin: " + servicePlugin.getName());
                servicePlugins.add(servicePlugin);
            } else {
                logger.warning("Service-plugin " + servicePlugin.getName() + " is already added!");
            }
        }
    }


    @Override
    public Set<IServicePlugin> getServicePlugins() {
        return Collections.unmodifiableSet(servicePlugins);
    }


    @Override
    public void onSessionOpen(final IServerSession session, final boolean isClient) throws IOException {
        if (sslPort != null && sslPort.intValue() == ((InetSocketAddress) session.getChannel().getLocalAddress()).getPort()) {
            session.setSecure(true);
        }

        if (session.isSecure()) {
            if (sslContext == null) {
                try {
                    sslContext = SSLContext.getInstance("SSL");

                    try {
                        if (isClient) {
                            sslContext.init(null, null, null);
                        } else {
                            final KeyManager[] keyManagers = SslSupport.createKeyManagers(keystoreFile, truststorePassword, keystorePassword);
                            final TrustManager[] trustManagers = SslSupport.createTrustManagers(truststoreFile, truststorePassword);

                            sslContext.init(keyManagers, trustManagers, new SecureRandom());
                        }
                    } catch (final Exception e) {
                        throw new IOException("Could not get initialize SSLContext!", e);
                    }

                    final SSLSession dummySession = sslContext.createSSLEngine().getSession();
                    SslSupport.setApplicationBufferSize(dummySession.getApplicationBufferSize());
                    SslSupport.setPacketBufferSize(dummySession.getPacketBufferSize());
                    readBufferSize = dummySession.getPacketBufferSize();
                    dummySession.invalidate();

                } catch (final Exception e) {
                    throw new IllegalStateException("Could not get instance of SSLContext!", e);
                }
            }

            try {
                SSLEngine sslEngine;

                if (isClient) {
                    final String host = session.getAttribute(SessionConstants.SESSION_SSL_CLIENT_HOST, String.class);
                    final Integer port = session.getAttribute(SessionConstants.SESSION_SSL_CLIENT_PORT, Integer.class);

                    if (host == null || port == null) {
                        throw new IOException("Peer host and port not specified for client SSL connection!");
                    }

                    sslEngine = sslContext.createSSLEngine(host, port);
                } else {
                    sslEngine = sslContext.createSSLEngine();
                }

                sslEngine.setUseClientMode(isClient);
                sslEngine.beginHandshake();

                if (!SslSupport.doHandshake(session.getChannel(), sslEngine)) {
                    session.getChannel().close();
                    logger.info("Connection closed due to handshake failure.");
                } else {
                    session.setAttribute(SessionConstants.SESSION_ATTRIBUTE_SSL_ENGINE, sslEngine);
                }
            } catch (final Exception e) {
                throw new IOException("Could not perform SSL handshake!", e);
            }
        }
    }


    @Override
    public void onSessionClose(final IServerSession session) throws IOException {
        final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_ATTRIBUTE_SSL_ENGINE, SSLEngine.class);
        if (sslEngine != null) {
            try {
                sslEngine.closeInbound();
            } catch (final Exception e) {
            }
            try {
                sslEngine.closeOutbound();
            } catch (final Exception e) {
            }
        }
    }


    @Override
    public ByteBuffer unwrap(final IServerSession session, final ByteBuffer buffer) throws IOException {
        if (session.isSecure()) {
            final ByteBuffer unwrapBuffer = ByteBuffer.allocate(buffer.capacity());
            final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_ATTRIBUTE_SSL_ENGINE, SSLEngine.class);

            final SSLEngineResult result = sslEngine.unwrap(buffer, unwrapBuffer);

            switch (result.getStatus()) {
                case OK:
                    unwrapBuffer.flip();
                    return unwrapBuffer;
                case BUFFER_OVERFLOW:
                    return SslSupport.enlargeApplicationBuffer(sslEngine, unwrapBuffer);
                case BUFFER_UNDERFLOW:
                    return SslSupport.handleBufferUnderflow(sslEngine, buffer);
                case CLOSED:
                    logger.fine("Closing SSL connection...");
                    SslSupport.closeConnection(session.getChannel(), sslEngine);
                    return null;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

        return buffer;
    }


    @Override
    public ByteBuffer[] wrap(final IServerSession session, final ByteBuffer... buffers) throws IOException {
        if (session.isSecure()) {
            final SSLEngine sslEngine = session.getAttribute(SessionConstants.SESSION_ATTRIBUTE_SSL_ENGINE, SSLEngine.class);
            final ByteBuffer[] wrappedBuffers = new ByteBuffer[buffers.length];

            for (int i = 0; i < buffers.length; i++) {
                final ByteBuffer buffer = buffers[i];
                boolean retry = false;

                do {
                    ByteBuffer wrappedBuffer = ByteBuffer.allocate(SslSupport.getPacketBufferSize());
                    final SSLEngineResult result = sslEngine.wrap(buffer, wrappedBuffer);
                    retry = false;

                    switch (result.getStatus()) {
                        case OK:
                            wrappedBuffer.flip();
                            wrappedBuffers[i] = wrappedBuffer;
                        break;
                        case BUFFER_OVERFLOW:
                            wrappedBuffer = SslSupport.enlargePacketBuffer(sslEngine, wrappedBuffer);
                            retry = true;
                        break;
                        case BUFFER_UNDERFLOW:
                            throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                        case CLOSED:
                            return null;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                } while (retry);
            }

            return wrappedBuffers;
        }

        return buffers;
    }


    @Override
    public int getReadBufferSize() {
        return readBufferSize;
    }


    protected Class<? extends IIOHandler> findMapping(final String path) {
        for (final IServicePlugin plugin : servicePlugins) {
            if (IServicePluginHttp.class.isAssignableFrom(plugin.getClass())) {
                final Class<? extends IIOHandler> handlerClass = ((IServicePluginHttp) plugin).findMapping(path);

                if (handlerClass != null) {
                    return handlerClass;
                }
            }
        }

        return HttpIOHandler.class;
    }


    @Override
    public Pair<DetermineStatus, IIOHandler> determineIoHandler(final SessionAttributes sessionAttributes, final ByteBuffer buffer) throws IOException {
        IIOHandler handler = null;
        boolean needMoreData = false;
        String line;

        try {
            line = BufferUtil.readLine(buffer, Charset.forName("ASCII"));

            final Iterator<IServicePlugin> it = servicePlugins.iterator();

            while (handler == null && it.hasNext()) {
                final IServicePlugin plugin = it.next();

                try {
                    buffer.reset();

                    final Pair<DetermineStatus, IIOHandler> result = plugin.determineIoHandler(buffer, sessionAttributes);

                    switch (result.getValue1()) {
                        case FALSE:
                        break;

                        case TRUE: {
                            if (result.getValue2() != null) {
                                handler = result.getValue2();
                            }
                        }
                        break;

                        case NEED_MORE_DATA: {
                            needMoreData = true;
                        }
                        break;
                        default:
                        break;
                    }
                } catch (final Exception e) {
                    // ignore quietly and proceed with search

                } finally {
                    buffer.reset();
                }
            }
        } finally {
            buffer.reset();
        }

        if (handler != null) {
            logger.info("Using " + handler.getClass().getName() + " for request: " + line);
            return new Pair<>(DetermineStatus.TRUE, handler);
        } else if (needMoreData) {
            return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
        } else {
            // if no handler was mapped, use default HttpIOHandler
            handler = serverContext.getCdiContainer().select(HttpIOHandler.class).get();
            logger.info("Module " + getName() + " could not find mapping for: " + line);
            logger.info("Using default " + handler.getClass().getName() + " for request: " + line);
            return new Pair<>(DetermineStatus.TRUE, handler);
        }
    }

}
