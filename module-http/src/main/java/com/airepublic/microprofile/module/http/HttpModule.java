package com.airepublic.microprofile.module.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.PostConstruct;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.DetermineStatus;
import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.core.IServicePlugin;
import com.airepublic.microprofile.core.Pair;
import com.airepublic.microprofile.core.ServerContext;
import com.airepublic.microprofile.core.ServerSession;
import com.airepublic.microprofile.module.http.core.HttpBufferUtils;
import com.airepublic.microprofile.module.http.core.IServicePluginHttp;

@ApplicationScoped
public class HttpModule implements IServerModule {
    private final static Logger LOG = LoggerFactory.getLogger(HttpModule.class);
    public final static String PORT = "http.port";
    public final static String SSL_PORT = "http.ssl.port";
    public final static String KEYSTORE_FILE = "http.keystore.file";
    public final static String KEYSTORE_PASSWORD = "http.keystore.password";
    public final static String TRUSTSTORE_FILE = "http.truststore.file";
    public final static String TRUSTSTORE_PASSWORD = "http.truststore.password";
    private final static String SESSION_ATTRIBUTE_SSL_ENGINE = "http.sslEngine";
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
    private ServerContext serverContext;
    private SSLContext sslContext;
    private int readBufferSize = 16 * 1024;


    protected ServerContext getServerContext() {
        return serverContext;
    }


    @Override
    public String getName() {
        return "Http module";
    }


    @PostConstruct
    public void initModule() {
        if (sslPort != null) {
            try {
                sslContext = SSLContext.getInstance("TLSv1.2");

                try {
                    final KeyManager[] keyManagers = SslSupport.createKeyManagers(keystoreFile, truststorePassword, keystorePassword);
                    final TrustManager[] trustManagers = SslSupport.createTrustManagers(truststoreFile, truststorePassword);

                    sslContext.init(keyManagers, trustManagers, new SecureRandom());
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
    public void addServicePlugins(final Set<IServicePlugin> plugins) {
        if (plugins != null) {
            for (final IServicePlugin featurePlugin : plugins) {
                if (!servicePlugins.contains(featurePlugin)) {
                    LOG.info("\tAdding feature-plugin: " + featurePlugin.getName());
                    servicePlugins.add(featurePlugin);
                } else {
                    LOG.warn("Feature-plugin " + featurePlugin.getName() + " is already added!");
                }
            }
        }
    }


    boolean isSecure(final ServerSession session) throws IOException {
        return sslPort != null && session != null && sslPort.intValue() == ((InetSocketAddress) session.getChannel().getLocalAddress()).getPort();
    }


    @Override
    public void onAccept(final ServerSession session) throws IOException {

        if (isSecure(session)) {
            try {
                final SSLEngine sslEngine = sslContext.createSSLEngine();
                sslEngine.setUseClientMode(false);
                sslEngine.beginHandshake();

                if (!SslSupport.doHandshake(session.getChannel(), sslEngine)) {
                    session.getChannel().close();
                    LOG.info("Connection closed due to handshake failure.");
                }

                session.setAttribute(SESSION_ATTRIBUTE_SSL_ENGINE, sslEngine);
            } catch (final Exception e) {
                throw new IOException("Could not perform SSL handshake!", e);
            }

        }
    }


    @Override
    public ByteBuffer unwrap(final ServerSession session, final ByteBuffer buffer) throws IOException {
        if (isSecure(session)) {
            final ByteBuffer unwrapBuffer = ByteBuffer.allocate(buffer.capacity());
            final SSLEngine sslEngine = (SSLEngine) session.getAttribute(SESSION_ATTRIBUTE_SSL_ENGINE);

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
                    LOG.debug("Client wants to close connection...");
                    SslSupport.closeConnection(session.getChannel(), sslEngine);
                    session.close();
                    LOG.debug("Goodbye client!");
                    return null;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

        return buffer;
    }


    @Override
    public ByteBuffer[] wrap(final ServerSession session, final ByteBuffer... buffers) throws IOException {
        if (isSecure(session)) {
            final SSLEngine sslEngine = (SSLEngine) session.getAttribute(SESSION_ATTRIBUTE_SSL_ENGINE);
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


    protected Class<? extends AbstractIOHandler> findMapping(final String path) {
        for (final IServicePlugin plugin : servicePlugins) {
            if (IServicePluginHttp.class.isAssignableFrom(plugin.getClass())) {
                final Class<? extends AbstractIOHandler> handlerClass = ((IServicePluginHttp) plugin).findMapping(path);

                if (handlerClass != null) {
                    return handlerClass;
                }
            }
        }

        return HttpIOHandler.class;
    }


    @Override
    public Pair<DetermineStatus, AbstractIOHandler> determineIoHandler(final ByteBuffer buffer, final ServerSession session) throws IOException {
        final String path = HttpBufferUtils.getUriPath(buffer);

        if (path == null) {
            return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
        }

        AbstractIOHandler handler = null;
        boolean needMoreData = false;
        final Iterator<IServicePlugin> it = servicePlugins.iterator();

        while (handler == null && it.hasNext()) {
            final IServicePlugin plugin = it.next();
            final Pair<DetermineStatus, AbstractIOHandler> result = plugin.determineIoHandler(buffer, session);

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
        }

        if (handler != null) {
            LOG.info("Using " + handler.getClass().getName() + " for request: " + path);
            return new Pair<>(DetermineStatus.TRUE, handler);
        } else if (needMoreData) {
            return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
        } else {
            // if no handler was mapped, use default HttpIOHandler
            handler = serverContext.getCdiContainer().select(HttpIOHandler.class).get();
            handler.init(session);
            LOG.info("Module " + getName() + " could not find mapping for: " + path);
            LOG.info("Using default " + handler.getClass().getName() + " for request: " + path);
            return new Pair<>(DetermineStatus.TRUE, handler);
        }
    }

}
