package com.airepublic.microprofile.module.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.airepublic.http.common.SslSupport;
import com.airepublic.microprofile.core.spi.IChannelProcessor;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.Request;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.core.spi.SessionConstants;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

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
    private SSLContext clientSslContext;
    private SSLContext serverSslContext;
    private int readBufferSize = 16 * 1024;
    private final HttpIOHandler defaultIOHandler = new HttpIOHandler();


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

        final HttpChannelEncoder channelEncoder = new HttpChannelEncoder();
        channelEncoder.init(processor.getChannel(), getServerSslContext(), isSecure);
        processor.getSession().setAttribute(SessionConstants.SESSION_SSL_ENGINE, channelEncoder.getSslEngine());
        processor.setChannelEncoder(channelEncoder);
        processor.getChannel().getRemoteAddress();
        processor.getChannel().register(processor.getSelector(), SelectionKey.OP_READ);

        ForkJoinPool.commonPool().submit(processor);
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
    public IIOHandler determineIoHandler(final Request request) throws IOException {
        return servicePlugins.stream().map(plugin -> plugin.determineIoHandler(request)).filter(plugin -> plugin != null).findFirst().orElse(defaultIOHandler);
    }


    SSLContext getServerSslContext() {
        return serverSslContext;
    }


    SSLContext getClientSslContext() {
        return clientSslContext;
    }
}
