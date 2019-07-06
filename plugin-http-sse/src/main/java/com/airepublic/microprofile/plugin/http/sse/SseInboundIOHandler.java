package com.airepublic.microprofile.plugin.http.sse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.ws.rs.sse.InboundSseEvent;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.util.http.common.AsyncHttpRequestReader;
import com.airepublic.microprofile.util.http.common.BufferUtil;
import com.airepublic.microprofile.util.http.common.Headers;
import com.airepublic.microprofile.util.http.common.HttpRequest;

@SessionScoped
public class SseInboundIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    private final AsyncHttpRequestReader requestReader = new AsyncHttpRequestReader();
    @Inject
    private IServerSession session;
    private final AtomicBoolean isHandshakeDone = new AtomicBoolean(false);
    private SseEventSourceImpl sseEventSource;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);


    @Override
    public ChannelAction consume(final ByteBuffer buffer) throws IOException {
        if (!isInitialized.get()) {
            sseEventSource = (SseEventSourceImpl) session.getAttribute(SsePlugin.SSE_SESSION_EVENTSOURCE);

            if (sseEventSource == null) {
                throw new IOException("SseEventSource for inbound event not found for key " + SsePlugin.SSE_SESSION_EVENTSOURCE + " in session attributes!");
            }

            isInitialized.set(true);
        }

        List<InboundSseEvent> events;

        try {
            events = parseInboundSseEvent(buffer);

            events.forEach(sseEventSource::onEvent);
        } catch (final Exception e) {
            sseEventSource.onError(e);
        }

        return ChannelAction.KEEP_OPEN;
    }


    /**
     * Parses the {@link ByteBuffer} and creates {@link InboundSseEvent}(s) found in the buffer. The
     * buffer will be reset to the last fully read {@link InboundSseEvent}.
     * 
     * @param buffer the {@link ByteBuffer}
     * @return a list of parsed {@link InboundSseEvent}
     * @throws IOException if something goes wrong
     */
    private List<InboundSseEvent> parseInboundSseEvent(final ByteBuffer buffer) throws IOException {
        final List<InboundSseEvent> events = new ArrayList<>();
        String line = "";
        InboundSseEventImpl event = new InboundSseEventImpl();

        buffer.mark();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            while (line != null && buffer.hasRemaining()) {
                String name = "";
                String value = "";
                char chr = ' ';
                char previousChr;

                line = BufferUtil.readNextToken(buffer, "\n", Charset.forName("UTF-8"));

                for (int i = 0; i < line.length(); i++) {
                    previousChr = chr;
                    chr = line.charAt(i);

                    // check for key/value separator
                    if (chr == ':') {
                        name = new String(bos.toByteArray(), "UTF-8").strip();
                        bos.reset();
                    } else if (chr == '\n') { // check for end of key/value
                        // check if end of event (\n\n)
                        if (previousChr == '\n') {
                            events.add(event);
                            event = new InboundSseEventImpl();

                            buffer.mark();
                        } else {
                            value = new String(bos.toByteArray(), "UTF-8").stripLeading();
                            bos.reset();

                            if (name.equalsIgnoreCase("event")) {
                                event.setName(value);
                            } else if (name.equalsIgnoreCase("id")) {
                                event.setId(value);
                            } else if (name.equalsIgnoreCase("retry")) {
                                event.setReconnectDelay(Long.valueOf(value));
                            } else if (name.equalsIgnoreCase("data")) {
                                event.setData(event.readData() + value);
                            } else if (name.isBlank()) {
                                event.setComment(value);
                            }
                        }
                    } else {
                        bos.write(chr);
                    }
                }
            }
        }

        // check if the buffer has only one event without separator (\n\n)
        if (event.readData() != null && event.readData().length() > 0 && !events.contains(event)) {
            events.add(event);
        } else {
            buffer.reset();
        }
        return events;
    }


    @Override
    public void produce() throws IOException {
        if (!isHandshakeDone.get()) {
            doHandshake(session.getChannel());
        }
    }


    protected void doHandshake(final SocketChannel channel) throws IOException {
        sseEventSource = (SseEventSourceImpl) session.getAttribute(SsePlugin.SSE_SESSION_EVENTSOURCE);

        final Headers headers = new Headers();
        headers.add(Headers.CONTENT_TYPE, "text/event-stream");
        headers.add(Headers.CONNECTION, "keep-alive");
        headers.add(Headers.CACHE_CONTROL, "no-cache");

        final HttpRequest request = new HttpRequest(sseEventSource.getUri(), headers);
        request.withMethod("GET");
        request.withVersion("HTTP/1.1");
        session.addToWriteBuffer(request.getHeaderBuffer());
    }


    @Override
    public ChannelAction onReadError(final Throwable t) {
        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public void handleClosedInput() throws IOException {
    }


    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
        // output not needed after handshake
        // session.getChannel().shutdownOutput();
        session.getSelectionKey().interestOps(SelectionKey.OP_READ);
        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }

}
