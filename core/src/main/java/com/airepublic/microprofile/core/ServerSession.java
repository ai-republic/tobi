package com.airepublic.microprofile.core;

import java.io.IOException;
import java.net.StandardSocketOptions;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSession implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ServerSession.class);
    private ServerContext serverContext;
    private Selector selector;
    private SelectionKey key;
    private SSLEngine sslEngine;
    private AbstractIOHandler ioHandler;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Queue<ByteBuffer> in = new ConcurrentLinkedQueue<>();
    private final Queue<Pair<ByteBuffer[], CompletionHandler<?, ?>>> out = new ConcurrentLinkedQueue<>();
    private final SSLContext sslContext;
    private final boolean isSecure;
    private final SocketChannel channel;


    public ServerSession(final SocketChannel channel, final boolean isSecure, final SSLContext sslContext, final ServerContext serverContext) throws IOException {
        this.serverContext = serverContext;
        this.isSecure = isSecure;
        this.sslContext = sslContext;
        this.channel = channel;
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


    public SSLEngine getSslEngine() {
        return sslEngine;
    }


    void setSslEngine(final SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }


    public boolean isSecure() {
        return isSecure;
    }


    public AbstractIOHandler getIoHandler() {
        return ioHandler;
    }


    void setIoHandler(final AbstractIOHandler handler) {
        ioHandler = handler;
    }


    public boolean isClosed() {
        return closed.get();
    }


    @Override
    public void run() {
        try {
            channel.configureBlocking(false);

            if (isSecure) {
                try {
                    sslEngine = sslContext.createSSLEngine();
                    sslEngine.setUseClientMode(false);
                    sslEngine.beginHandshake();

                    if (!SslSupport.doHandshake(channel, sslEngine)) {
                        channel.close();
                        LOG.info("Connection closed due to handshake failure.");
                    }
                } catch (final Exception e) {
                    throw new IOException("Could not perform SSL handshake!", e);
                }
            }

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
                    final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    final Iterator<SelectionKey> it = selectedKeys.iterator();

                    while (it.hasNext()) {
                        final SelectionKey key = it.next();
                        it.remove();

                        if (key == null) {
                            return;
                        }

                        if (key.isValid() && key.isReadable()) {
                            handleRead();
                        }

                        if (key.isValid() && key.isWritable()) {
                            handleWrite();
                        }

                        if (!key.isValid()) {
                            close();
                        }
                    }
                } catch (final Exception e) {
                    LOG.error("Error processing request!", e);
                }
            }
        } finally {
            LOG.info("Shutting down server session!");
            close();
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
            ByteBuffer buffer = ByteBuffer.allocate(SslSupport.getPacketBufferSize());
            final SocketChannel channel = getChannel();

            final int len = channel.read(buffer);
            buffer.flip();

            if (len == -1) {
                // input stream has been closed
                handleAction(ChannelAction.CLOSE_ALL);
                return;
            }

            if (isSecure()) {
                ByteBuffer unwrapBuffer = ByteBuffer.allocate(buffer.capacity());
                final SSLEngineResult result = getSslEngine().unwrap(buffer, unwrapBuffer);

                switch (result.getStatus()) {
                    case OK:
                        unwrapBuffer.flip();
                        buffer = unwrapBuffer;
                    break;
                    case BUFFER_OVERFLOW:
                        unwrapBuffer = SslSupport.enlargeApplicationBuffer(getSslEngine(), unwrapBuffer);
                    break;
                    case BUFFER_UNDERFLOW:
                        buffer = SslSupport.handleBufferUnderflow(getSslEngine(), buffer);
                    break;
                    case CLOSED:
                        LOG.debug("Client wants to close connection...");
                        SslSupport.closeConnection(channel, getSslEngine());
                        LOG.debug("Goodbye client!");
                        return;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

            AbstractIOHandler handler = getIoHandler();

            if (handler == null) {
                try {
                    final Pair<DetermineStatus, AbstractIOHandler> pair = determineHandler(buffer);

                    if (pair.getValue1() == DetermineStatus.TRUE && pair.getValue2() != null) {
                        handler = pair.getValue2();
                    } else if (pair.getValue1() == DetermineStatus.NEED_MORE_DATA) {
                        action = ChannelAction.KEEP_OPEN;
                    }
                } catch (final Exception e) {
                    action = ChannelAction.CLOSE_ALL;
                }

                if (handler != null) {
                    setIoHandler(handler);
                } else {
                    LOG.error("No matching handler found for: " + buffer);
                    action = ChannelAction.CLOSE_ALL;
                    return;
                }
            }

            if (len > 0) {
                try {
                    action = handler.consume(buffer);
                } catch (final Exception e) {
                    action = handler.onReadError(e);
                }
            } else {
                action = ChannelAction.KEEP_OPEN;
            }

            handleAction(action);
        } catch (final Exception e) {
            LOG.error("Exception during read processing: ", e);
            close();
        }
    }


    private void handleWrite() throws IOException {
        final AbstractIOHandler handler = getIoHandler();

        try {
            if (handler == null) {
                throw new IOException("Handler has not been initialized!");
            }

            final ChannelAction action = handler.produce();

            Pair<ByteBuffer[], CompletionHandler<?, ?>> pair = getNextWriteBuffer();

            while (pair != null) {
                if (pair.getValue1() != null) {
                    try {
                        final SocketChannel channel = getChannel();
                        final ByteBuffer[] buffers = pair.getValue1();

                        if (isSecure()) {

                            for (int i = 0; i < buffers.length; i++) {
                                final ByteBuffer buffer = buffers[i];
                                boolean retry = false;

                                do {
                                    ByteBuffer wrappedBuffer = ByteBuffer.allocate(SslSupport.getPacketBufferSize());
                                    final SSLEngineResult result = getSslEngine().wrap(buffer, wrappedBuffer);
                                    retry = false;

                                    switch (result.getStatus()) {
                                        case OK:
                                            wrappedBuffer.flip();
                                            buffers[i] = wrappedBuffer;
                                        break;
                                        case BUFFER_OVERFLOW:
                                            wrappedBuffer = SslSupport.enlargePacketBuffer(getSslEngine(), wrappedBuffer);
                                            retry = true;
                                        break;
                                        case BUFFER_UNDERFLOW:
                                            throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                                        case CLOSED:
                                            handleAction(ChannelAction.CLOSE_ALL);
                                        default:
                                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                                    }
                                } while (retry);
                            }
                        }

                        final long length = channel.write(buffers);

                        if (pair.getValue2() != null) {
                            handler.writeSuccessful(pair.getValue2(), length);
                        }
                    } catch (final Throwable t) {
                        if (pair.getValue2() != null) {
                            handler.writeFailed(pair.getValue2(), t);
                        }
                    }
                }

                pair = getNextWriteBuffer();
            }

            handleAction(action);
        } catch (final Exception e) {
            LOG.error("Exception during write processing: ", e);
            handleAction(ChannelAction.CLOSE_ALL);
        }
    }


    Pair<DetermineStatus, AbstractIOHandler> determineHandler(final ByteBuffer buffer) throws IOException {
        final Class<? extends AbstractIOHandler> handlerClass = null;
        boolean needMoreData = false;

        for (final IServerModule module : serverContext.getModules()) {
            final Pair<DetermineStatus, AbstractIOHandler> pair = module.determineHandlerClass(buffer, this);

            switch (pair.getValue1()) {
                case TRUE:
                    return pair;

                case FALSE:
                break;

                case NEED_MORE_DATA:
                    needMoreData = true;
                break;
            }
        }

        return new Pair<>(needMoreData ? DetermineStatus.NEED_MORE_DATA : DetermineStatus.FALSE, null);
    }


    public void close() {
        if (!isClosed()) {
            key.cancel();

            if (getSslEngine() != null) {
                try {
                    SslSupport.closeConnection(getChannel(), getSslEngine());
                    sslEngine.closeInbound();
                    sslEngine.closeOutbound();
                } catch (final IOException e) {
                }
            }

            try {
                key.channel().close();
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
            sslEngine = null;

            closed.set(true);
        }
    }


    SocketChannel getChannel() {
        return channel;// (SocketChannel) getKey().channel();
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
