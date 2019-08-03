package com.airepublic.microprofile.core;

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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import com.airepublic.microprofile.core.spi.IChannelProcessor;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

@ApplicationScoped
public class JavaServer {
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


    private void init(final SeContainer cdiContainer) {
        if (!initialized.get()) {
            synchronized (initialized) {
                if (!initialized.get()) {
                    // add shutdown hook to clean up resources
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            JavaServer.this.stop();
                        }
                    });

                    try {
                        // serverContext.setCdiContainer(cdiContainer);

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

                                serverContext.addModule(module);
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


    public void start(final SeContainer cdiContainer) throws IOException {
        if (!initialized.get()) {
            synchronized (initialized) {
                if (!initialized.get()) {
                    init(cdiContainer);
                }
            }
        }

        run();
    }


    @Asynchronous
    protected Future<Void> run() throws IOException {
        if (!running.get()) {
            synchronized (running) {
                if (!running.get()) {
                    running.set(true);
                } else {
                    throw new IOException(getClass().getSimpleName() + " is already running!");
                }
            }
        }

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

        stop();

        return null;
    }


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


    void accept(final SelectionKey connectionKey) throws IOException {
        if (!shouldConnect(connectionKey)) {
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
            IServerSession session = serverContext.getServerSession(channel.getRemoteAddress());

            if (session == null) {
                session = CDI.current().select(IServerSession.class).get();
            }

            final IChannelProcessor processor = CDI.current().select(IChannelProcessor.class).get();
            processor.prepare(session, module, channel, null);
            module.accept(processor);

            // final SessionContainer sc = CDI.current().select(SessionContainer.class).get();
            // sc.startSession(moduleForKey.get(connectionKey), () -> channel, new
            // SessionAttributes(), false);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error creating session", e);
        } finally {
        }
    }


    boolean shouldConnect(final SelectionKey connectionKey) {
        return true;
    }

}
