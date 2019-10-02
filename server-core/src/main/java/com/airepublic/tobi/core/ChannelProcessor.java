package com.airepublic.tobi.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.BeanContextStorage;
import com.airepublic.tobi.core.spi.ChannelAction;
import com.airepublic.tobi.core.spi.IChannelEncoder;
import com.airepublic.tobi.core.spi.IChannelEncoder.Status;
import com.airepublic.tobi.core.spi.IChannelProcessor;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IRequest;
import com.airepublic.tobi.core.spi.IResponse;
import com.airepublic.tobi.core.spi.IServerContext;
import com.airepublic.tobi.core.spi.IServerModule;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Pair;
import com.airepublic.tobi.core.spi.RequestScopedContext;
import com.airepublic.tobi.core.spi.SessionScopedContext;

/**
 * The {@link IChannelProcessor} implementation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ChannelProcessor implements IChannelProcessor {
    private static AtomicLong SESSION_ID_GENERATOR = new AtomicLong();
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private IServerModule module;
    private SocketChannel channel;
    private final Selector selector;
    private IIOHandler ioHandler;
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private IChannelEncoder channelEncoder;
    private IServerSession session;
    private String sessionId;
    @Inject
    private IServerContext serverContext;
    @Inject
    private RequestScopedContext requestScopedContext;
    @Inject
    private SessionScopedContext sessionScopedContext;

    /**
     * Constructor.
     * 
     * @throws IOException if a {@link Selector} could not be opened
     */
    public ChannelProcessor() throws IOException {
        selector = Selector.open();
    }


    /**
     * Re-initializes the {@link ChannelProcessor} to be reused.
     * 
     * @param module the {@link IServerModule}
     * @param channel the {@link SocketChannel}
     * @param ioHandler the {@link IIOHandler}
     * @throws IOException if the channel is already closed
     */
    @Override
    public void prepare(final IServerModule module, final SocketChannel channel, final IIOHandler ioHandler) throws IOException {
        final BeanContextStorage sessionContext = new BeanContextStorage();
        requestScopedContext.activate(new BeanContextStorage());
        sessionScopedContext.activate(sessionContext);

        session = CDI.current().select(IServerSession.class).get();
        session.setId("" + SESSION_ID_GENERATOR.incrementAndGet());
        serverContext.addServerSession(session);

        // reset
        closing.set(false);
        closed.set(false);

        session.setChannelProcessor(this);

        this.module = module;
        this.channel = channel;
        this.ioHandler = ioHandler;
    }


    @Override
    public IServerSession getSession() {
        return session;
    }


    @Override
    public IChannelEncoder getChannelEncoder() {
        return channelEncoder;
    }


    @Override
    public void setChannelEncoder(final IChannelEncoder channelEncoder) {
        this.channelEncoder = channelEncoder;
    }


    @Override
    public void run() {

        logger.info("Starting channel processing for module '" + module.getName() + "' session #" + session.getId());

        while (getSelector().isOpen() && !closing.get() && !closed.get()) {
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
                logger.log(Level.SEVERE, "Error processing request for session #" + session.getId(), e);
            }
        }

        close();

    }


    /**
     * Handles the specified {@link ChannelAction}.
     * 
     * @param action the {@link ChannelAction}
     * @throws IOException if something goes wrong
     */
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


    /**
     * Handles reading from the connection.
     * 
     * @throws IOException if something goes wrong
     */
    protected void handleRead() throws IOException {
        try {
            ChannelAction action = ChannelAction.KEEP_OPEN;
            final ByteBuffer buffer = ByteBuffer.allocate(module.getReadBufferSize());
            final SocketChannel channel = getChannel();

            if (channel.isOpen()) {
                final int len = channel.read(buffer);
                buffer.flip();

                if (len == -1) {
                    // input stream has been closed
                    handleAction(ChannelAction.CLOSE_ALL);
                    return;
                }

                buffer.mark();
                final byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                logger.info("read raw --> " + new String(bytes));
                buffer.reset();
                // Thread.sleep(3000);

                final Pair<Status, IRequest> result = channelEncoder.decode(buffer);

                if (result.getValue1() == Status.FULLY_READ) {
                    final IRequest request = result.getValue2();
                    session.setRequest(request);

                    module.checkAuthorization(session);

                    if (getIoHandler() == null) {
                        setIoHandler(module.determineIoHandler(request));
                        logger.info("Using '" + getIoHandler().getClass().getSimpleName() + "' to process session #" + session.getId());
                    }

                    if (len > 0) {
                        try {
                            action = getIoHandler().consume(request);
                        } catch (final Exception e) {
                            action = getIoHandler().onReadError(e);
                        }
                    } else {
                        action = ChannelAction.KEEP_OPEN;
                    }

                    handleAction(action);
                } else if (result.getValue1() == Status.NEED_MORE_DATA) {
                    // need more data to receive request
                    handleAction(ChannelAction.KEEP_OPEN);
                } else if (result.getValue1() == Status.CLOSED) {
                    handleAction(ChannelAction.CLOSE_ALL);
                }
            } else {
                handleAction(ChannelAction.CLOSE_ALL);
            }
        } catch (final SecurityException e) {
            logger.log(Level.WARNING, "Exception during read processing in session #" + session.getId() + ". Closing connection: " + e.getLocalizedMessage(), e);
            handleAction(ChannelAction.CLOSE_ALL);
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Exception during read processing in session #" + session.getId() + ". Closing connection: " + e.getLocalizedMessage(), e);

            handleAction(ChannelAction.CLOSE_ALL);
        }
    }


    /**
     * Handles writing to the connection.
     * 
     * @throws IOException if something goes wrong
     */
    protected void handleWrite() throws IOException {
        if (getIoHandler() == null) {
            handleAction(ChannelAction.CLOSE_ALL);
            throw new IOException("IOHandler has not been set/found!");
        }

        try {

            final Pair<? extends IResponse, CompletionHandler<?, ?>> pair = getIoHandler().produce();

            if (pair != null && pair.getValue1() != null) {
                flush(pair);
            } else {
                getChannel().keyFor(getSelector()).interestOps(SelectionKey.OP_READ);
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Exception during write processing in module '" + module.getName() + "' session #" + session.getId() + ": ", e);
            handleAction(ChannelAction.CLOSE_ALL);
        }
    }


    /**
     * Flushes the write buffers to the connection.
     * 
     * @throws IOException if something goes wrong
     */
    protected void flush(final Pair<? extends IResponse, CompletionHandler<?, ?>> pair) throws IOException {
        synchronized (closed) {
            if (!closed.get()) {
                final IIOHandler handler = getIoHandler();

                if (handler == null) {
                    throw new IOException("Handler has not been initialized!");
                }

                // Pair<ByteBuffer[], CompletionHandler<?, ?>> pair = getNextWriteBuffer();
                ChannelAction action = ChannelAction.KEEP_OPEN;

                if (pair.getValue1() != null) {
                    try {
                        final SocketChannel channel = getChannel();
                        final IResponse response = pair.getValue1();
                        ByteBuffer[] buffers = null;

                        if (response.getAttributesBuffer() != null && response.getPayload() != null) {
                            buffers = new ByteBuffer[] { pair.getValue1().getAttributesBuffer(), pair.getValue1().getPayload() };
                        } else if (response.getAttributesBuffer() != null) {
                            buffers = new ByteBuffer[] { response.getAttributesBuffer() };
                        } else if (response.getPayload() != null) {
                            buffers = new ByteBuffer[] { response.getPayload() };
                        } else {
                            buffers = new ByteBuffer[] {};
                        }

                        long length = -1;

                        buffers = channelEncoder.encode(buffers);

                        if (channel.isOpen()) {
                            length = channel.write(buffers);
                        }

                        action = handler.writeSuccessful(pair.getValue2(), length);
                    } catch (final Throwable t) {
                        logger.log(Level.SEVERE, "Error writing buffers in module '" + module.getName() + "' session #" + session.getId() + ": " + t.getLocalizedMessage());
                        action = handler.writeFailed(pair.getValue2(), t);
                    }

                    handleAction(action);
                }
            }
        }
    }


    @Override
    public void close() {

        synchronized (closed) {
            if (closed.compareAndSet(false, true)) {
                logger.info("Closing channel for module '" + module.getName() + "' session #" + session.getId() + " !");

                session.close();

                try {
                    serverContext.removeServerSession(session);
                } catch (final IOException e) {
                }

                if (ioHandler != null) {
                    ioHandler.onSessionClose(session);
                    ioHandler = null;
                }

                if (sessionScopedContext.isActive()) {
                    sessionScopedContext.deactivate();
                }

                if (requestScopedContext.isActive()) {
                    requestScopedContext.deactivate();
                }

                getChannel().keyFor(getSelector()).cancel();

                try {
                    channelEncoder.close();
                } catch (final Exception e1) {
                }

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

                // out.clear();
                // in.clear();

                selector.wakeup();
            }
        }
    }


    @Override
    public IServerModule getModule() {
        return module;
    }


    @Override
    public Selector getSelector() {
        return selector;
    }


    @Override
    public SocketChannel getChannel() {
        return channel;
    }


    /**
     * Sets the {@link SocketChannel} to process.
     * 
     * @param channel the {@link SocketChannel}
     */
    void setChannel(final SocketChannel channel) {
        this.channel = channel;
    }


    @Override
    public IIOHandler getIoHandler() {
        return ioHandler;
    }


    /**
     * Sets the {@link IIOHandler}.
     * 
     * @param ioHandler the {@link IIOHandler}
     */
    protected void setIoHandler(final IIOHandler ioHandler) {
        this.ioHandler = ioHandler;
    }
}
