package com.airepublic.tobi.module.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.airepublic.http.common.SslSupport;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.IChannelProcessor;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IRequest;
import com.airepublic.tobi.core.spi.IServerModule;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.IServicePlugin;

/**
 * The module for handling HTTP/S requests and responses. The module can be configured with
 * following properties using the microprofile configuration:
 * <ul>
 * <li><code>http.port</code> - the port to use for HTTP</li>
 * <li><code>http.ssl.port</code> - the port to use for HTTPS</li>
 * <li><code>http.keystore.file</code> - the path to the keystore file</li>
 * <li><code>http.keystore.password</code> - the password to the keystore file</li>
 * <li><code>http.truststore.file</code> - the path to the truststore file</li>
 * <li><code>http.truststore.password</code> - the password to the truststore file</li>
 * </ul>
 * 
 * @author Torsten Oltmanns
 *
 */
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
    private final Set<IServicePlugin> servicePlugins = new TreeSet<>((p1, p2) -> Integer.compare(p1.getPriority(), p2.getPriority()));
    @Inject
    @ConfigProperty(name = PORT)
    private Integer port;
    @Inject
    @ConfigProperty(name = SSL_PORT)
    private Integer sslPort;
    @Inject
    @ConfigProperty(name = KEYSTORE_FILE, defaultValue = "~/keystore.jks")
    private String keystoreFile;
    @Inject
    @ConfigProperty(name = KEYSTORE_PASSWORD, defaultValue = "changeit")
    private String keystorePassword;
    @Inject
    @ConfigProperty(name = TRUSTSTORE_FILE, defaultValue = "~/cacerts.jks")
    private String truststoreFile;
    @Inject
    @ConfigProperty(name = TRUSTSTORE_PASSWORD, defaultValue = "changeit")
    private String truststorePassword;
    private SSLContext clientSslContext;
    private SSLContext serverSslContext;
    private int readBufferSize = 16 * 1024;
    @Inject
    private IHttpAuthorizationProvider authorizationProvider;

    /**
     * Initializes the {@link SSLContext}s.
     */
    @PostConstruct
    public void init() {

        try {
            serverSslContext = SslSupport.createServerSSLContext(keystoreFile, keystorePassword, truststoreFile, truststorePassword);
            clientSslContext = SslSupport.createClientSSLContext();

            readBufferSize = SslSupport.getPacketBufferSize();
        } catch (final IOException e) {
            throw new RuntimeException("Could not create SSL context:", e);
        }
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
    public void checkAuthorization(final IServerSession session) throws SecurityException, IOException {
        if (authorizationProvider != null) {
            authorizationProvider.accept(session);
        }
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
    public void accept(final IChannelProcessor processor) throws IOException {
        processor.getChannel().setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        processor.getChannel().setOption(StandardSocketOptions.SO_REUSEADDR, true);
        processor.getChannel().configureBlocking(false);

        boolean isSecure = false;

        if (sslPort != null && sslPort.intValue() == ((InetSocketAddress) processor.getChannel().getLocalAddress()).getPort()) {
            isSecure = true;
        }

        final HttpChannelEncoder channelEncoder = CDI.current().select(HttpChannelEncoder.class).get();
        channelEncoder.init(processor.getSession(), getServerSslContext(), isSecure);

        processor.getSession().setAttribute(SessionConstants.SESSION_SSL_ENGINE, channelEncoder.getSslEngine());
        processor.setChannelEncoder(channelEncoder);
        processor.getChannel().getRemoteAddress();
        processor.getChannel().register(processor.getSelector(), SelectionKey.OP_READ);
    }


    @Override
    public void close() throws IOException {
        for (final IServicePlugin plugin : servicePlugins) {
            try {
                plugin.close();
            } catch (final Exception e) {
            }
        }
    }


    @Override
    public int getReadBufferSize() {
        return readBufferSize;
    }


    @Override
    public IIOHandler determineIoHandler(final IRequest request) throws IOException {
        IIOHandler handler = servicePlugins.stream().map(plugin -> plugin.determineIoHandler(request)).filter(plugin -> plugin != null).findFirst().orElse(null);

        if (handler == null) {
            handler = CDI.current().select(HttpIOHandler.class).get();
            logger.info("No handler found for '" + ((HttpRequest) request).getRequestLine() + "'. Using default handler: " + handler.getClass().getSimpleName());
        }

        return handler;
    }


    /**
     * Gets the {@link SSLContext} initialized for server connections.
     * 
     * @return the {@link SSLContext}
     */
    SSLContext getServerSslContext() {
        return serverSslContext;
    }


    /**
     * Gets the {@link SSLContext} initialized for client connections.
     * 
     * @return the {@link SSLContext}
     */
    SSLContext getClientSslContext() {
        return clientSslContext;
    }
}
