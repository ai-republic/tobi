package com.airepublic.tobi.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.IChannelProcessor;
import com.airepublic.tobi.core.spi.IServerModule;
import com.airepublic.tobi.core.spi.IServicePlugin;

/**
 * The Tobi server implementation which will accept and process incoming connections. Initially it
 * will scan for {@link IServerModule}s and {@link IServicePlugin}s and configure the server
 * accordingly. Incoming connections will be processed by the {@link IChannelProcessor}.
 * 
 * @author Torsten Oltmanns
 *
 */
@ApplicationScoped
public class TobiServer {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private Selector selector;
    @Inject
    private ServerContext serverContext;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<SelectionKey, IServerModule> moduleForKey = new HashMap<>();
    private final Map<SelectionKey, ServerSocketChannel> serverSocketChannels = new HashMap<>();

    /**
     * Initializes the Tobi server by looking up {@link IServerModule}s and {@link IServicePlugin}s.
     */
    private void init() {
        if (!initialized.get()) {
            synchronized (initialized) {
                if (!initialized.get()) {
                    // add shutdown hook to clean up resources
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            TobiServer.this.stop();
                        }
                    });

                    try {
                        selector = Selector.open();
                        final String host = serverContext.getHost();
                        final List<Integer> openPorts = new ArrayList<>();

                        // load feature plugins and structure them by protocol
                        final Map<String, Set<Class<? extends IServicePlugin>>> servicePlugins = new HashMap<>();

                        final ServiceLoader<IServicePlugin> services = ServiceLoader.load(IServicePlugin.class);
                        services.forEach(p -> {
                            final IServicePlugin plugin = CDI.current().select(p.getClass()).get();

                            for (final String protocol : plugin.getSupportedProtocols()) {
                                Set<Class<? extends IServicePlugin>> protocolServicePlugins = servicePlugins.get(protocol);

                                if (protocolServicePlugins == null) {
                                    protocolServicePlugins = new HashSet<>();
                                    servicePlugins.put(protocol, protocolServicePlugins);
                                }

                                protocolServicePlugins.add(plugin.getClass());
                            }
                        });

                        // load server modules
                        final ServiceLoader<IServerModule> serviceLoader = ServiceLoader.load(IServerModule.class);

                        serviceLoader.forEach(m -> {
                            try {
                                logger.info("Starting " + m.getName() + "...");
                                final IServerModule module = CDI.current().select(m.getClass()).get();

                                // add service plugins for the supported protocol to the module
                                servicePlugins.get(module.getProtocol()).forEach(clazz -> {
                                    final IServicePlugin plugin = CDI.current().select(clazz).get();
                                    module.addServicePlugin(plugin);
                                });

                                final int[] ports = module.getPortsToOpen();

                                if (ports != null && ports.length > 0) {
                                    for (final int port : ports) {
                                        logger.info("Starting server for module " + module.getName() + " on " + host + ":" + port + "...");

                                        if (!openPorts.contains(Integer.valueOf(port))) {
                                            final ServerSocketChannel serverSocket = ServerSocketChannel.open();
                                            serverSocket.configureBlocking(false);
                                            serverSocket.bind(new InetSocketAddress(host, port));

                                            final SelectionKey key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);

                                            serverSocketChannels.put(key, serverSocket);
                                            moduleForKey.put(key, module);
                                            openPorts.add(Integer.valueOf(port));
                                        } else {
                                            logger.info("Module " + module.getName() + " is sharing the port " + port + " with another module!");
                                        }
                                    }
                                }

                                module.getServicePlugins().forEach(plugin -> plugin.initPlugin(module));
                            } catch (final Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

                        logger.info("Started server successfully!");
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, "Error starting server: ", e);
                    }
                }

                initialized.set(true);
            }
        }
    }


    /**
     * Starts the server.
     * 
     * @param runAfterStart a task to run after the server has started
     * @throws IOException if starting fails
     */
    public void start(final Runnable runAfterStart) throws IOException {
        if (!initialized.get()) {
            synchronized (initialized) {
                if (!initialized.get()) {
                    init();
                }
            }
        }

        if (!running.get()) {
            synchronized (running) {
                if (!running.get()) {
                    running.set(true);
                } else {
                    throw new IOException(getClass().getSimpleName() + " is already running!");
                }
            }
        }

        if (runAfterStart != null) {
            ForkJoinPool.commonPool().submit(runAfterStart);
        }

        try {
            while (running.get()) {
                selector.select();
                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> it = selectedKeys.iterator();

                while (it.hasNext()) {
                    final SelectionKey connectionKey = it.next();
                    it.remove();

                    if (!connectionKey.isValid()) {
                        continue;
                    }

                    if (connectionKey.isAcceptable()) {
                        accept(connectionKey);
                    }

                }
            }
        } finally {
            stop();
        }
    }


    /**
     * Stops the server.
     */
    public void stop() {
        if (running.get()) {
            synchronized (running) {
                if (running.get()) {
                    running.set(false);
                } else {
                    return;
                }
            }
        }

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

        for (final IServerModule module : moduleForKey.values()) {
            try {
                module.close();
            } catch (final Exception e) {
            }
        }
    }


    /**
     * Accepts the connection for the specified {@link SelectionKey}.
     * 
     * @param connectionKey the {@link SelectionKey}
     * @throws IOException if something goes wrong
     */
    void accept(final SelectionKey connectionKey) throws IOException {
        if (!shouldAllowConnect(connectionKey)) {
            connectionKey.cancel();
            throw new IOException("Should not connect!");
        }

        final ServerSocketChannel serverSocket = serverSocketChannels.get(connectionKey);
        final SocketChannel channel = serverSocket.accept();

        if (channel == null) {
            throw new IOException("Failed to accept connection (channel is null)!");
        }

        try {
            final IServerModule module = moduleForKey.get(connectionKey);

            final IChannelProcessor processor = CDI.current().select(IChannelProcessor.class).get();

            ForkJoinPool.commonPool().submit(() -> {
                try {
                    processor.prepare(module, channel, null);
                    module.accept(processor);
                    processor.run();
                } catch (final IOException e) {
                    logger.log(Level.SEVERE, "Error creating session", e);
                }
            });
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error accepting socket!", e);
        } finally {
        }
    }


    /**
     * Verifies whether the connection should be allowed.
     * 
     * @param connectionKey the {@link SelectionKey} for the connection
     * @return true if if should be allowed
     */
    boolean shouldAllowConnect(final SelectionKey connectionKey) {
        return true;
    }

}
