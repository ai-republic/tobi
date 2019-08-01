package com.airepublic.microprofile.core;

import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.IChannelEncoder;
import com.airepublic.microprofile.core.spi.IChannelEncoder.Status;
import com.airepublic.microprofile.core.spi.IChannelProcessor;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.Request;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.Pair;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

public class ChannelProcessor implements IChannelProcessor {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private IServerModule module;
    private SocketChannel channel;
    private final Selector selector;
    private IIOHandler ioHandler;
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Queue<ByteBuffer> in = new ConcurrentLinkedQueue<>();
    private final Queue<Pair<ByteBuffer[], CompletionHandler<?, ?>>> out = new ConcurrentLinkedQueue<>();
    private IChannelEncoder channelEncoder;
    private IServerSession session;
    @Inject
    private RequestScopedContext requestScopedContext;
    @Inject
    private SessionScopedContext sessionScopedContext;


    public ChannelProcessor() throws IOException {
        selector = Selector.open();
    }


    /**
     * Re-initializes the {@link ChannelProcessor} to be reused.
     * 
     * @param module the {@link HttpModule}
     * @param channel the {@link SocketChannel}
     * @param ioHandler the {@link IIOHandler}
     * @param isSecure flag whether the channel is SSL encrypted
     */
    @Override
    public void prepare(final IServerSession session, final IServerModule module, final SocketChannel channel, final IIOHandler ioHandler) {
        // reset
        closing.set(false);
        closed.set(false);

        this.session = session;
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
        requestScopedContext.activate(new RequestContext(Long.valueOf(session.getId())));

        // final Long sessionId = Long.valueOf(session.getId());
        // final SessionContext sessionContext = serverContext.getSessionContext(sessionId);

        // if (sessionContext == null) {
        // sessionContext = new SessionContext(sessionId);
        // serverContext.addSessionContext(sessionId, sessionContext);
        // }
        //
        // sessionScopedContext.activate(sessionContext);
        logger.info("Starting channel processing for module '" + module.getName() + "'");

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
                logger.log(Level.SEVERE, "Error processing request!", e);
            }
        }

        // sessionScopedContext.deactivate();
        requestScopedContext.deactivate();
        close();

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

                // buffer.mark();
                // final byte[] bytes = new byte[buffer.remaining()];
                // buffer.get(bytes);
                // logger.info("read raw --> " + new String(bytes));
                // buffer.reset();
                // Thread.sleep(3000);

                final Pair<Status, Request> result = channelEncoder.decode(buffer);

                if (result.getValue1() == Status.FULLY_READ) {
                    final Request request = result.getValue2();

                    if (getIoHandler() == null) {
                        setIoHandler(module.determineIoHandler(request));
                        logger.info("Using '" + getIoHandler() + "' to process: " + request);
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

                } else {
                    // need more data to receive request
                    handleAction(ChannelAction.KEEP_OPEN);
                }

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
            handleAction(ChannelAction.CLOSE_ALL);
            throw new IOException("IOHandler has not been set/found!");
        }

        try {

            getIoHandler().produce();

            if (out.isEmpty()) {
                getChannel().keyFor(getSelector()).interestOpsAnd(SelectionKey.OP_READ);
            } else {
                flush();
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Exception during write processing in module " + module.getName() + ": ", e);
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
                            long length = -1;

                            buffers = channelEncoder.encode(buffers);

                            if (channel.isOpen()) {
                                length = channel.write(buffers);
                            }

                            action = handler.writeSuccessful(pair.getValue2(), length);
                        } catch (final Throwable t) {
                            logger.log(Level.SEVERE, "Error writing buffers in module " + module.getName() + ": " + t.getLocalizedMessage());
                            action = handler.writeFailed(pair.getValue2(), t);
                        }

                        handleAction(action);
                    }

                    if (!closing.get() && !closed.get()) {
                        pair = getNextWriteBuffer();
                    } else {
                        pair = null;
                    }
                }
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
            getChannel().keyFor(getSelector()).interestOpsOr(SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }


    protected ByteBuffer getNextReadBuffer() {
        return in.poll();
    }


    protected Pair<ByteBuffer[], CompletionHandler<?, ?>> getNextWriteBuffer() {
        if (out.size() > 0) {
            return out.poll();
        }

        return null;
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
            if (closed.compareAndSet(false, true)) {
                logger.info("Closing channel for module '" + module.getName() + "'!");

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

                out.clear();
                in.clear();

                selector.wakeup();

                ioHandler.onSessionClose();
                ioHandler = null;
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


    void setChannel(final SocketChannel channel) {
        this.channel = channel;
    }


    @Override
    public IIOHandler getIoHandler() {
        return ioHandler;
    }


    protected void setIoHandler(final IIOHandler ioHandler) {
        this.ioHandler = ioHandler;
        this.ioHandler.setSession(session);
    }
}
