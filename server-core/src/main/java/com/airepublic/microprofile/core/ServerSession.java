package com.airepublic.microprofile.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SessionScoped
public class ServerSession implements Closeable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ServerSession.class);
    private ServerContext serverContext;
    private long id;
    private Selector selector;
    private SelectionKey key;
    private AbstractIOHandler ioHandler;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Queue<ByteBuffer> in = new ConcurrentLinkedQueue<>();
    private final Queue<Pair<ByteBuffer[], CompletionHandler<?, ?>>> out = new ConcurrentLinkedQueue<>();
    private SocketChannel channel;
    private IServerModule module;
    private final Map<String, Object> attributes = new HashMap<>();


    public void init(final long id, final IServerModule module, final SocketChannel channel, final ServerContext serverContext) throws IOException {
        this.id = id;
        this.module = module;
        this.channel = channel;
        this.serverContext = serverContext;
    }


    public long getId() {
        return id;
    }


    public ServerContext getServerContext() {
        return serverContext;
    }


    void setServerContext(final ServerContext serverContext) {
        this.serverContext = serverContext;
    }


    Selector getSelector() {
        return selector;
    }


    public SelectionKey getKey() {
        return key;
    }


    public AbstractIOHandler getIoHandler() {
        return ioHandler;
    }


    void setIoHandler(final AbstractIOHandler handler) {
        ioHandler = handler;
    }


    public SocketChannel getChannel() {
        return channel;
    }


    public boolean isClosed() {
        return closed.get();
    }


    public void setAttribute(final String key, final Object value) {
        attributes.put(key, value);
    }


    public Object getAttribute(final String key) {
        return attributes.get(key);
    }


    public void run() {
        try {
            channel.configureBlocking(false);
            module.onAccept(this);

            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

            selector = Selector.open();
            key = channel.register(selector, SelectionKey.OP_READ, this);
        } catch (final Exception e) {
            close();

            throw new RuntimeException("Failed to initialize " + getClass().getName(), e);
        }

        try {

            while (selector.isOpen()) {
                try {
                    selector.select();

                    if (selector.isOpen()) {
                        final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        final Iterator<SelectionKey> it = selectedKeys.iterator();

                        while (it.hasNext()) {
                            final SelectionKey key = it.next();
                            it.remove();

                            if (key == null) {
                                continue;
                            }

                            if (key.isValid() && key.isReadable()) {
                                handleRead();
                            }

                            if (key.isValid() && key.isWritable()) {
                                handleWrite();
                            }

                            if (!key.isValid()) {
                                handleAction(ChannelAction.CLOSE_ALL);
                            }
                        }
                    }
                } catch (final Exception e) {
                    LOG.error("Error processing request!", e);
                }
            }
        } finally {
            try {
                handleAction(ChannelAction.CLOSE_ALL);
            } catch (final IOException e) {
                // ignore quietly
            }
        }
    }


    private void handleAction(final ChannelAction action) throws IOException {
        final SocketChannel channel = getChannel();

        switch (action) {
            case KEEP_OPEN:
            break;
            case CLOSE_INPUT:
                channel.shutdownInput();

                if (getIoHandler() != null) {
                    getIoHandler().handleClosedInput();
                }
            break;
            case CLOSE_OUTPUT:
                channel.shutdownOutput();
            break;
            case CLOSE_ALL:
                close();
            break;
        }
    }


    private void handleRead() throws IOException {

        try {
            ChannelAction action = ChannelAction.KEEP_OPEN;
            ByteBuffer buffer = ByteBuffer.allocate(module.getReadBufferSize());
            final SocketChannel channel = getChannel();

            final int len = channel.read(buffer);
            buffer.flip();

            if (len == -1) {
                // input stream has been closed
                handleAction(ChannelAction.CLOSE_ALL);
                return;
            }

            buffer = module.unwrap(this, buffer);

            // if the IO handler is null, this is the first read
            if (getIoHandler() == null) {
                try {
                    // need to check which module has registered a handler that can handle the
                    // initial buffer
                    action = determineIoHandler(buffer);
                } catch (final Exception e) {
                    action = ChannelAction.CLOSE_ALL;
                }
            }

            if (getIoHandler() != null) {
                if (len > 0) {
                    try {
                        action = getIoHandler().consume(buffer);
                    } catch (final Exception e) {
                        action = getIoHandler().onReadError(e);
                    }
                } else {
                    action = ChannelAction.KEEP_OPEN;
                }
            }

            handleAction(action);
        } catch (final Exception e) {
            LOG.error("Exception during read processing: ", e);
            handleAction(ChannelAction.CLOSE_ALL);
        }
    }


    private void handleWrite() throws IOException {
        final AbstractIOHandler handler = getIoHandler();

        try {
            if (handler == null) {
                throw new IOException("Handler has not been initialized!");
            }

            handler.produce();

            Pair<ByteBuffer[], CompletionHandler<?, ?>> pair = getNextWriteBuffer();
            ChannelAction action = ChannelAction.KEEP_OPEN;

            while (pair != null) {
                if (pair.getValue1() != null) {
                    try {
                        final SocketChannel channel = getChannel();
                        ByteBuffer[] buffers = pair.getValue1();

                        buffers = module.wrap(this, buffers);

                        final long length = channel.write(buffers);

                        action = handler.writeSuccessful(pair.getValue2(), length);
                    } catch (final Throwable t) {
                        LOG.error("Error writing buffers for session #" + getId() + " in module " + module.getName() + ": ", t);
                        action = handler.writeFailed(pair.getValue2(), t);
                    }

                    handleAction(action);
                }

                if (!isClosed()) {
                    pair = getNextWriteBuffer();
                } else {
                    pair = null;
                }
            }

        } catch (final Exception e) {
            LOG.error("Exception during write processing for session #" + getId() + " in module " + module.getName() + ": ", e);
            handleAction(ChannelAction.CLOSE_ALL);
        }
    }


    ChannelAction determineIoHandler(final ByteBuffer buffer) throws IOException {
        final Pair<DetermineStatus, AbstractIOHandler> pair = module.determineIoHandler(buffer, this);

        switch (pair.getValue1()) {
            case FALSE:
                return ChannelAction.CLOSE_ALL;

            case TRUE:
                if (pair.getValue2() != null) {
                    setIoHandler(pair.getValue2());
                    return ChannelAction.KEEP_OPEN;
                } else {
                    return ChannelAction.CLOSE_ALL;
                }

            case NEED_MORE_DATA: {
                return ChannelAction.KEEP_OPEN;
            }
            default:
                return ChannelAction.CLOSE_ALL;
        }
    }


    @Override
    public void close() {
        synchronized (closed) {
            if (!isClosed()) {
                key.cancel();

                try {
                    if (getChannel().isOpen()) {
                        getChannel().close();
                    }
                } catch (final IOException e) {
                }

                try {
                    selector.close();
                } catch (final IOException e) {
                }

                serverContext.removeServerSession(this);
                serverContext = null;
                ioHandler = null;
                key = null;
                out.clear();
                in.clear();

                closed.set(true);
                selector.wakeup();
            }
        }
    }


    public synchronized void addToReadBuffer(final ByteBuffer... buffer) {
        Stream.of(buffer).forEach(in::add);
    }


    public synchronized void addToWriteBuffer(final ByteBuffer... buffer) {
        addToWriteBuffer(null, buffer);
    }


    public synchronized void addToWriteBuffer(final CompletionHandler<?, ?> handler, final ByteBuffer... buffer) {
        // remove all empty buffers
        final List<ByteBuffer> filtered = Stream.of(buffer).filter(b -> b != null && b.limit() > 0).collect(Collectors.toList());

        // if there are any left to add then add
        if (filtered != null && !filtered.isEmpty()) {
            final Pair<ByteBuffer[], CompletionHandler<?, ?>> pair = new Pair<>(filtered.toArray(new ByteBuffer[filtered.size()]), handler);
            out.add(pair);
        }

        // if there are buffers in the out queue then wake up selector for writing
        if (out.size() > 0) {
            key.interestOpsOr(SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }


    public synchronized ByteBuffer getNextReadBuffer() {
        return in.poll();
    }


    public synchronized Pair<ByteBuffer[], CompletionHandler<?, ?>> getNextWriteBuffer() {
        if (out.size() > 0) {
            return out.poll();
        } else {
            key.interestOpsAnd(SelectionKey.OP_READ);
        }

        return null;
    }

}
