package com.airepublic.microprofile.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
import com.airepublic.microprofile.core.spi.SessionAttributes;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

@SessionScoped
public class ServerSession implements Closeable, Serializable, IServerSession {
    private static final long serialVersionUID = 1L;
    private static AtomicLong SESSION_ID_GENERATOR = new AtomicLong();

    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private ServerContext serverContext;
    private String id;
    private Selector selector;
    private SelectionKey selectionKey;
    private IIOHandler ioHandler;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final Queue<ByteBuffer> in = new ConcurrentLinkedQueue<>();
    private final Queue<Pair<ByteBuffer[], CompletionHandler<?, ?>>> out = new ConcurrentLinkedQueue<>();
    private SocketChannel channel;
    private IServerModule module;
    private SessionAttributes attributes;


    @Override
    public String getId() {
        return id;
    }


    protected Selector getSelector() {
        return selector;
    }


    @Override
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }


    @Override
    public SocketChannel getChannel() {
        return channel;
    }


    @Override
    public boolean isSecure() {
        final Boolean isSecure = getAttribute(IServerSession.SESSION_IS_SECURE, Boolean.class);

        if (isSecure != null && isSecure == Boolean.TRUE) {
            return true;
        }

        return false;
    }


    @Override
    public void setSecure(final boolean isSecure) {
        setAttribute(SESSION_IS_SECURE, isSecure);
    }


    @Override
    public boolean isClosed() {
        return closed.get();
    }


    @Override
    public void setAttribute(final String key, final Object value) {
        attributes.set(key, value);
    }


    @Override
    public <T> T getAttribute(final String key, final Class<T> type) {
        return attributes.get(key, type);
    }


    @Override
    public void open(final IServerModule module, final SocketChannel channel, final SessionAttributes sessionAttributes, final boolean isClient) throws IOException {
        if (id == null) {
            id = "" + SESSION_ID_GENERATOR.incrementAndGet();
            selector = Selector.open();
        }

        attributes = sessionAttributes;
        this.module = module;
        this.channel = channel;

        try {
            module.onSessionOpen(this, isClient);

            if (isClient) {
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

                if (getIoHandler() == null) {
                    final Pair<DetermineStatus, IIOHandler> pair = determineIOHandler(buffer);

                    if (pair.getValue1() == DetermineStatus.TRUE) {
                        setIoHandler(pair.getValue2());
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
                } else {
                    handleAction(ChannelAction.CLOSE_ALL);
                    throw new IOException("IIOHandler has not been initialized!");

                }

                handleAction(action);
            } else {
                handleAction(ChannelAction.CLOSE_ALL);
            }
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Exception during read processing. Closing input channel: " + e.getLocalizedMessage());

            if (getIoHandler() != null) {
                handleAction(getIoHandler().onReadError(e));
            } else {
                handleAction(ChannelAction.CLOSE_ALL);
            }
        }
    }


    protected void handleWrite() throws IOException {
        if (getIoHandler() == null) {
            final Pair<DetermineStatus, IIOHandler> pair = determineIOHandler(null);

            if (pair.getValue1() == DetermineStatus.TRUE) {
                setIoHandler(pair.getValue2());
            }
        }

        if (getIoHandler() == null) {
            handleAction(ChannelAction.CLOSE_ALL);
            throw new IOException("IIOHandler has not been set/found!");
        }

        try {

            getIoHandler().produce();

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


    protected IIOHandler getIoHandler() {
        return ioHandler;
    }


    protected void setIoHandler(final IIOHandler handler) {
        ioHandler = handler;
        setAttribute(SESSION_IO_HANDLER, handler);
    }


    @SuppressWarnings("unchecked")
    protected Pair<DetermineStatus, IIOHandler> determineIOHandler(final ByteBuffer buffer) {
        if (ioHandler == null) {
            // first check if a handler class or handler has been set in the session attributes
            IIOHandler handler = getAttribute(SESSION_IO_HANDLER, IIOHandler.class);

            if (handler == null) {
                final Class<? extends IIOHandler> handlerClass = getAttribute(SESSION_IO_HANDLER_CLASS, Class.class);

                if (handlerClass != null) {
                    try {
                        handler = CDI.current().select(handlerClass).get();
                        return new Pair<>(DetermineStatus.TRUE, handler);
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, "IOHandler class '" + handlerClass + "' is set in session attributes, but cannot be instantiated by CDI!", e);
                    }

                } else {
                    try {
                        buffer.mark();

                        return module.determineIoHandler(attributes, buffer);
                    } catch (final Exception e) {
                        logger.log(Level.WARNING, "Module " + module.getName() + " threw an exception while checking if it can handle a channel!", e);
                    } finally {
                        buffer.reset();
                    }
                }
            } else {
                return new Pair<>(DetermineStatus.TRUE, handler);
            }

            return new Pair<>(DetermineStatus.FALSE, null);
        }

        return new Pair<>(DetermineStatus.TRUE, ioHandler);
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

                try {
                    module.onSessionClose(this);
                } catch (final IOException e1) {
                }

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
                selectionKey = null;
                out.clear();
                in.clear();

                closed.set(true);
                selector.wakeup();

                ioHandler.onSessionClose();
                ioHandler = null;
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
