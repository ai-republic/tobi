package com.airepublic.microprofile.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JavaServer implements IConfigConstants {
    private static final Logger LOG = LoggerFactory.getLogger(JavaServer.class);

    private Selector selector;
    private ServerSocketChannel serverSocket;
    private ServerSocketChannel sslServerSocket;
    private SSLContext sslContext;
    @Inject
    private ServerContext serverContext;
    @Inject
    private Config config;
    private boolean running = false;
    private SelectionKey key;
    private SelectionKey sslKey;
    private ExecutorService executor;


    void init() throws IOException {

        try {

            initServer();

            final ServiceLoader<IServerModule> serviceLoader = ServiceLoader.load(IServerModule.class);
            serviceLoader.forEach(module -> {
                try {
                    module.initModule(config, serverContext);
                    serverContext.addModule(module);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });

            LOG.info("Started server successfully!");
        } catch (final Exception e) {
            LOG.error("Error starting server: ", e);
        }
    }


    void initServer() throws IOException {
        final String host = serverContext.getHost();
        final int port = serverContext.getPort();
        final int sslPort = serverContext.getSslPort();

        LOG.info("Starting server on " + host + ":" + port + "/" + sslPort + "...");

        executor = Executors.newWorkStealingPool(serverContext.getWorkerCount());

        selector = Selector.open();

        LOG.info("Starting HTTP connector...");
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.configureBlocking(false);
        key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        try {
            sslContext = SSLContext.getInstance("TLSv1.2");

            try {
                final KeyManager[] keyManagers = SslSupport.createKeyManagers("D:/keystore.jks", serverContext.getTruststorePassword(), serverContext.getKeystorePassword());
                final TrustManager[] trustManagers = SslSupport.createTrustManagers("D:/cacerts.jks", serverContext.getTruststorePassword());

                sslContext.init(keyManagers, trustManagers, new SecureRandom());
            } catch (final Exception e) {
                throw new IOException("Could not get initialize SSLContext!", e);
            }

            final SSLSession dummySession = sslContext.createSSLEngine().getSession();
            SslSupport.setApplicationBufferSize(dummySession.getApplicationBufferSize());
            SslSupport.setPacketBufferSize(dummySession.getPacketBufferSize());
            dummySession.invalidate();

        } catch (final Exception e) {
            throw new IOException("Could not get instance of SSLContext!", e);
        }

        sslServerSocket = ServerSocketChannel.open();
        sslServerSocket.configureBlocking(false);
        sslServerSocket.bind(new InetSocketAddress(host, sslPort));
        sslKey = sslServerSocket.register(selector, SelectionKey.OP_ACCEPT);
    }


    public void run() throws IOException {

        running = true;

        while (running) {
            selector.select();
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
            final Iterator<SelectionKey> it = selectedKeys.iterator();

            while (it.hasNext()) {
                final SelectionKey connectionKey = it.next();

                if (!connectionKey.isValid()) {
                    continue;
                }

                accept(connectionKey);
                it.remove();
            }
        }
    }


    public void stop() throws IOException {
        serverSocket.close();
        selector.close();
        running = false;
    }


    void accept(final SelectionKey connectionKey) throws IOException {
        if (!shouldConnect(connectionKey)) {
            connectionKey.cancel();
            throw new IOException("Should not connect!");
        }

        SocketChannel channel = null;
        boolean isSecure = false;

        if (connectionKey == key) {
            channel = serverSocket.accept();
        } else if (connectionKey == sslKey) {
            channel = sslServerSocket.accept();
            isSecure = true;
        }

        if (channel == null) {
            throw new IOException("Failed not accept connection (channel is null)!");
        }

        executor.execute(new ServerSession(channel, isSecure, sslContext, serverContext));
    }


    boolean shouldConnect(final SelectionKey connectionKey) {
        return true;
    }
}
