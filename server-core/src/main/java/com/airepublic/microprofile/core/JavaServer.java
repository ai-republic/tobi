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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class JavaServer {
    private static final Logger LOG = LoggerFactory.getLogger(JavaServer.class);

    private Selector selector;
    @Inject
    private ServerContext serverContext;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<SelectionKey, IServerModule> moduleForKey = new HashMap<>();
    private final Map<SelectionKey, ServerSocketChannel> serverSocketChannels = new HashMap<>();


    protected void init() {
        if (!initialized.get()) {
            synchronized (initialized) {
                if (!initialized.get()) {
                    try {
                        selector = Selector.open();
                        final String host = serverContext.getHost();
                        final List<Integer> openPorts = new ArrayList<>();

                        // load feature plugins and structure them by protocol
                        final Map<String, Set<IServicePlugin>> servicePlugins = new HashMap<>();

                        final ServiceLoader<IServicePlugin> services = ServiceLoader.load(IServicePlugin.class);
                        services.forEach(p -> {
                            final IServicePlugin plugin = serverContext.getCdiContainer().select(p.getClass()).get();

                            for (final String protocol : plugin.getSupportedProtocols()) {
                                Set<IServicePlugin> protocolServicePlugins = servicePlugins.get(protocol);

                                if (protocolServicePlugins == null) {
                                    protocolServicePlugins = new HashSet<>();
                                    servicePlugins.put(protocol, protocolServicePlugins);
                                }

                                protocolServicePlugins.add(plugin);
                            }
                        });

                        // load server modules
                        final ServiceLoader<IServerModule> serviceLoader = ServiceLoader.load(IServerModule.class);

                        serviceLoader.forEach(m -> {
                            try {
                                LOG.info("Starting " + m.getName() + "...");
                                final IServerModule module = serverContext.getCdiContainer().select(m.getClass()).get();

                                // add service plugins for the supported protocol to the module
                                module.addServicePlugins(servicePlugins.get(module.getProtocol()));

                                final int[] ports = module.getPortsToOpen();

                                if (ports != null && ports.length > 0) {
                                    for (final int port : ports) {
                                        LOG.info("Starting server for module " + module.getName() + " on " + host + ":" + port + "...");

                                        if (!openPorts.contains(Integer.valueOf(port))) {
                                            final ServerSocketChannel serverSocket = ServerSocketChannel.open();
                                            serverSocket.bind(new InetSocketAddress(host, port));
                                            serverSocket.configureBlocking(false);

                                            final SelectionKey key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);

                                            serverSocketChannels.put(key, serverSocket);
                                            moduleForKey.put(key, module);
                                            openPorts.add(Integer.valueOf(port));
                                        } else {
                                            LOG.info("Module " + module.getName() + " is sharing the port " + port + " with another module!");
                                        }
                                    }
                                }

                                serverContext.addModule(module);
                            } catch (final Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

                        LOG.info("Started server successfully!");
                    } catch (final Exception e) {
                        LOG.error("Error starting server: ", e);
                    }
                }

                initialized.set(true);
            }
        }
    }


    public void start() throws IOException {
        if (!initialized.get()) {
            synchronized (initialized) {
                if (!initialized.get()) {
                    init();
                }
            }
        }

        run();
    }


    @Asynchronous
    protected void run() throws IOException {
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
            final SessionContainer sessionContainer = serverContext.getCdiContainer().select(SessionContainer.class).get();
            sessionContainer.init(moduleForKey.get(connectionKey), channel);
            sessionContainer.run();
        } catch (final Exception e) {
            LOG.error("Error creating session", e);
        } finally {
        }
    }


    boolean shouldConnect(final SelectionKey connectionKey) {
        return true;
    }

}
