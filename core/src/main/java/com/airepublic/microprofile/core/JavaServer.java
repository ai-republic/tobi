package com.airepublic.microprofile.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JavaServer implements IConfigConstants {
    private static final Logger LOG = LoggerFactory.getLogger(JavaServer.class);

    private Selector selector;
    @Inject
    private ServerContext serverContext;
    @Inject
    private Config config;
    private boolean running = false;
    private SelectionKey key;
    private ExecutorService executor;
    private final Map<SelectionKey, IServerModule> moduleForKey = new HashMap<>();
    private final Map<SelectionKey, ServerSocketChannel> serverSocketChannels = new HashMap<>();


    void init() throws IOException {

        try {
            executor = Executors.newWorkStealingPool(serverContext.getWorkerCount());
            selector = Selector.open();
            final String host = serverContext.getHost();
            final List<Integer> openPorts = new ArrayList<>();
            final ServiceLoader<IServerModule> serviceLoader = ServiceLoader.load(IServerModule.class);

            serviceLoader.forEach(module -> {
                try {
                    LOG.info("Starting " + module.getName() + "...");
                    module.initModule(config, serverContext);

                    final int[] ports = module.getPortsToOpen();

                    if (ports != null && ports.length > 0) {
                        for (final int port : ports) {
                            LOG.info("Starting server for module " + module.getName() + " on " + host + ":" + port + "...");

                            if (!openPorts.contains(Integer.valueOf(port))) {
                                final ServerSocketChannel serverSocket = ServerSocketChannel.open();
                                serverSocket.bind(new InetSocketAddress(host, port));
                                serverSocket.configureBlocking(false);
                                key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
                                serverSocketChannels.put(key, serverSocket);
                                moduleForKey.put(key, module);
                                openPorts.add(Integer.valueOf(port));
                            }
                        }
                    }
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


    public void stop() {
        running = false;

        try {
            selector.close();
        } catch (final IOException e1) {
        }

        for (final ServerSocketChannel serverSocket : serverSocketChannels.values()) {
            try {
                serverSocket.close();
            } catch (final IOException e) {
            }
        }
    }


    void accept(final SelectionKey connectionKey) throws IOException {
        if (!shouldConnect(connectionKey)) {
            connectionKey.cancel();
            throw new IOException("Should not connect!");
        }

        final ServerSocketChannel serverSocket = serverSocketChannels.get(connectionKey);
        final SocketChannel channel = serverSocket.accept();

        if (channel == null) {
            throw new IOException("Failed not accept connection (channel is null)!");
        }

        executor.execute(new ServerSession(moduleForKey.get(connectionKey), channel, serverContext));
    }


    boolean shouldConnect(final SelectionKey connectionKey) {
        return true;
    }
}
