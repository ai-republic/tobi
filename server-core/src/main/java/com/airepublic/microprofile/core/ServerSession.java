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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.DetermineStatus;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.Pair;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

@SessionScoped
public class ServerSession implements Closeable, Serializable, IServerSession {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private ServerContext serverContext;
    private long id;
    private Selector selector;
    private SelectionKey selectionKey;
    private IIOHandler ioHandler;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final Queue<ByteBuffer> in = new ConcurrentLinkedQueue<>();
    private final Queue<Pair<ByteBuffer[], CompletionHandler<?, ?>>> out = new ConcurrentLinkedQueue<>();
    private SocketChannel channel;
    private IServerModule module;
    private final Map<String, Object> attributes = new HashMap<>();


    @Override
    public long getId() {
        return id;
    }


    protected Selector getSelector() {
        return selector;
    }


    @Override
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }


    protected IIOHandler getIoHandler() {
        return ioHandler;
    }


    protected void setIoHandler(final IIOHandler handler) {
        ioHandler = handler;
    }


    @Override
    public SocketChannel getChannel() {
        return channel;
    }


    @Override
    public boolean isClosed() {
        return closed.get();
    }


    @Override
    public void setAttribute(final String key, final Object value) {
        attributes.put(key, value);
    }


    @Override
    public Object getAttribute(final String key) {
        return attributes.get(key);
    }


    @Override
    public void open(final long id, final IServerModule module, final SocketChannel channel, final Map<String, Object> attributes, final boolean isOutbound) throws IOException {
        this.id = id;
        this.module = module;
        this.channel = channel;
        this.attributes.putAll(attributes);

        try {
            channel.configureBlocking(false);
            module.onAccept(this);

            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

            selector = Selector.open();

            if (isOutbound) {
                selectionKey = channel.register(selector, SelectionKey.OP_WRITE, this);
            } else {
                selectionKey = channel.register(selector, SelectionKey.OP_READ, this);
            }

        } catch (final Exception e) {
            throw new RuntimeException("Failed to initialize session #" + id, e);
        }
    }


    protected void handleIO() {

        while (getSelector().isOpen() && !closing.get() && !isClosed()) {
            try {
                getSelector().select();

                if (getSelector().isOpen()) {
                    final Set<SelectionKey> selectedKeys = getSelector().selectedKeys();
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
                logger.log(Level.SEVERE, "Error processing request!", e);
            }
        }
    }


    protected void handleAction(final ChannelAction action) throws IOException {
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
                closing.set(true);
            break;
        }
    }


    protected void handleRead() throws IOException {

        try {
            ChannelAction action = ChannelAction.KEEP_OPEN;
            ByteBuffer buffer = ByteBuffer.allocate(module.getReadBufferSize());
            final SocketChannel channel = getChannel();

            if (channel.isOpen()) {
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
            } else {
                handleAction(ChannelAction.CLOSE_ALL);
            }
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Exception during read processing. Closing input channel: " + e.getLocalizedMessage());
            handleAction(ChannelAction.CLOSE_INPUT);
        }
    }


    protected void handleWrite() throws IOException {
        IIOHandler handler = getIoHandler();

        if (handler == null) {
            determineIoHandler();

            handler = getIoHandler();

            if (handler == null) {
                handleAction(ChannelAction.CLOSE_ALL);
                throw new IOException("Handler has not been initialized!");
            }
        }

        try {

            handler.produce();

            if (out.isEmpty()) {
                selectionKey.interestOpsAnd(SelectionKey.OP_READ);
            } else {
                flush();
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Exception during write processing for session #" + getId() + " in module " + module.getName() + ": ", e);
            handleAction(ChannelAction.CLOSE_ALL);
        }
    }


    protected void flush() throws IOException {
        synchronized (closed) {
            if (!closed.get()) {
                final IIOHandler handler = getIoHandler();

                if (handler == null) {
                    throw new IOException("Handler has not been initialized!");
                }

                Pair<ByteBuffer[], CompletionHandler<?, ?>> pair = getNextWriteBuffer();
                ChannelAction action = ChannelAction.KEEP_OPEN;

                while (pair != null) {
                    if (pair.getValue1() != null) {
                        try {
                            final SocketChannel channel = getChannel();
                            ByteBuffer[] buffers = pair.getValue1();

                            buffers = module.wrap(this, buffers);

                            long length = -1;

                            if (channel.isOpen()) {
                                length = channel.write(buffers);
                            }

                            action = handler.writeSuccessful(pair.getValue2(), length);
                        } catch (final Throwable t) {
                            logger.log(Level.SEVERE, "Error writing buffers for session #" + getId() + " in module " + module.getName() + ": ", t);
                            action = handler.writeFailed(pair.getValue2(), t);
                        }

                        handleAction(action);
                    }

                    if (!closing.get() && !isClosed()) {
                        pair = getNextWriteBuffer();
                    } else {
                        pair = null;
                    }
                }
            }
        }
    }


    void determineIoHandler() throws IOException {
        // first check if a handler class or handler has been set in the session attributes
        IIOHandler handler = (IIOHandler) getAttribute(SESSION_IO_HANDLER);

        if (handler == null) {
            final Class<? extends IIOHandler> handlerClass = (Class<? extends IIOHandler>) getAttribute(SESSION_IO_HANDLER_CLASS);

            if (handlerClass != null) {
                try {
                    handler = CDI.current().select(handlerClass).get();
                } catch (final Exception e) {
                    throw new IOException("IOHandler class is set in session attributes, but cannot be instantiated by CDI!", e);
                }

                setIoHandler(handler);
            }
        }
    }


    ChannelAction determineIoHandler(final ByteBuffer buffer) throws IOException {

        // otherwise let the module and its plugins try to determine the handler
        buffer.mark();

        Pair<DetermineStatus, IIOHandler> pair;

        try {
            pair = module.determineIoHandler(buffer, this);
        } finally {
            buffer.reset();
        }

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
        // flush all output
        try {
            flush();
        } catch (final IOException e1) {
            // ignore quietly
        }

        synchronized (closed) {
            if (!isClosed()) {
                logger.info("Closing session #" + id + " for module '" + module.getName() + "'!");
                selectionKey.cancel();

                try {
                    if (getChannel().isOpen()) {
                        getChannel().close();
                    }
                } catch (final IOException e) {
                }

                try {
                    getSelector().close();
                } catch (final IOException e) {
                }

                serverContext.removeServerSession(this);
                serverContext = null;
                ioHandler = null;
                selectionKey = null;
                out.clear();
                in.clear();

                closed.set(true);
                selector.wakeup();
            }
        }
    }


    @Override
    public synchronized void addToReadBuffer(final ByteBuffer... buffer) {
        Stream.of(buffer).forEach(in::add);
    }


    @Override
    public synchronized void addToWriteBuffer(final ByteBuffer... buffer) {
        addToWriteBuffer(null, buffer);
    }


    @Override
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
            selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }


    @Override
    public ByteBuffer getNextReadBuffer() {
        return in.poll();
    }


    @Override
    public Pair<ByteBuffer[], CompletionHandler<?, ?>> getNextWriteBuffer() {
        if (out.size() > 0) {
            return out.poll();
        }

        return null;
    }

}
