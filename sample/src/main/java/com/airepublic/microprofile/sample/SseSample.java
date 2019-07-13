package com.airepublic.microprofile.sample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import com.airepublic.microprofile.module.http.SslSupport;
import com.airepublic.microprofile.plugin.http.sse.SseAsyncEventReader;
import com.airepublic.microprofile.plugin.http.sse.SseRepeat;
import com.airepublic.microprofile.plugin.http.sse.SseUri;
import com.airepublic.microprofile.util.http.common.AsyncHttpReader;
import com.airepublic.microprofile.util.http.common.Headers;
import com.airepublic.microprofile.util.http.common.HttpRequest;

@Path("/sse")
public class SseSample {
    @Inject
    private Sse sse;
    private int counter = 0;
    private static ByteBuffer unprocessedBuffer;


    @Produces("text/event-stream")
    @Path("/produce")
    @SseRepeat(delay = 1, unit = TimeUnit.SECONDS, maxTimes = 5)
    public void produce(@Context final SseEventSink sink) {

        final String[] words = new String[] { "Hello", "World", "from", "the", "SSE" };
        final OutboundSseEvent event = sse.newEventBuilder().name("MyEvent").reconnectDelay(1000L).data(words[counter % 5]).build();
        counter++;

        sink.send(event);
        // sink.close();
    }


    @Consumes("text/event-stream")
    @SseUri("https://api.boerse-frankfurt.de:443/data/price_information?isin=US00724F1012&mic=XFRA")
    public void consumes(final InboundSseEvent event) {
        System.out.println("received event: " + event);
    }


    public static void main(final String[] args) {
        SocketChannel channel;

        try {
            final SocketAddress remote = new InetSocketAddress("api.boerse-frankfurt.de", 443);
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(remote);

            while (!channel.finishConnect()) {
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create socket channel!", e);
        }

        SSLContext sslContext = null;
        int readBufferSize = 16000;

        try {
            sslContext = SSLContext.getInstance("SSL");

            try {
                // final KeyManager[] keyManagers = SslSupport.createKeyManagers(keystoreFile,
                // truststorePassword, keystorePassword);
                // final TrustManager[] trustManagers =
                // SslSupport.createTrustManagers(truststoreFile, truststorePassword);

                sslContext.init(null, null, null);
            } catch (final Exception e) {
                throw new IOException("Could not get initialize SSLContext!", e);
            }

            final SSLSession dummySession = sslContext.createSSLEngine().getSession();
            SslSupport.setApplicationBufferSize(dummySession.getApplicationBufferSize());
            SslSupport.setPacketBufferSize(dummySession.getPacketBufferSize());
            readBufferSize = dummySession.getPacketBufferSize();
            dummySession.invalidate();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        Selector selector = null;

        try {
            selector = Selector.open();
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            SSLEngine sslEngine;

            sslEngine = sslContext.createSSLEngine("api.boerse-frankfurt.de", 443);

            sslEngine.setUseClientMode(true);
            sslEngine.beginHandshake();

            if (!SslSupport.doHandshake(channel, sslEngine)) {
                channel.close();
            }

            final Headers headers = new Headers();
            headers.add(Headers.HOST, "api.boerse-frankfurt.de");
            headers.add(Headers.ACCEPT, "*/*");

            final HttpRequest request = new HttpRequest(new URI("https://api.boerse-frankfurt.de:443/data/price_information?isin=US00724F1012&mic=XFRA"), headers);
            ByteBuffer buffer = wrap(sslEngine, request.getHeaderBuffer());
            channel.write(buffer);

            channel.register(selector, SelectionKey.OP_READ);

            boolean responseOk = false;
            final SseAsyncEventReader eventReader = new SseAsyncEventReader();

            while (channel.isOpen()) {
                selector.select();

                final Iterator<SelectionKey> it = selector.keys().iterator();

                while (it.hasNext()) {
                    final SelectionKey key = it.next();

                    if (key.isValid() && key.isReadable()) {
                        buffer = ByteBuffer.allocate(readBufferSize);
                        final int len = channel.read(buffer);

                        if (len == -1) {
                            throw new IOException("Channel closed!");
                        }

                        buffer.flip();
                        buffer = unwrap(sslEngine, channel, buffer);
                        System.out.println(buffer.limit() + " bytes read");

                        if (!responseOk) {
                            final AsyncHttpReader response = new AsyncHttpReader();
                            if (response.receiveBuffer(buffer)) {
                                responseOk = true;
                                System.out.println("Received response: " + response.getHttpResponse());
                            }
                        } else {
                            final byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            if (eventReader.receiveBuffer(ByteBuffer.wrap(bytes))) {
                                System.out.println(eventReader.poll());
                            }
                        }
                        System.out.println("done");
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            try {
                channel.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }

            try {
                selector.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static ByteBuffer wrap(final SSLEngine sslEngine, final ByteBuffer buffer) throws IOException {

        boolean retry = false;

        do {
            ByteBuffer wrappedBuffer = ByteBuffer.allocate(SslSupport.getPacketBufferSize());
            final SSLEngineResult result = sslEngine.wrap(buffer, wrappedBuffer);
            retry = false;

            switch (result.getStatus()) {
                case OK:
                    wrappedBuffer.flip();
                    return wrappedBuffer;
                case BUFFER_OVERFLOW:
                    wrappedBuffer = SslSupport.enlargePacketBuffer(sslEngine, wrappedBuffer);
                    retry = true;
                break;
                case BUFFER_UNDERFLOW:
                    throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                case CLOSED:
                    return null;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        } while (retry);

        return buffer;
    }


    public static ByteBuffer unwrap(final SSLEngine sslEngine, final SocketChannel channel, final ByteBuffer buffer) throws IOException {
        final ByteBuffer unwrapBuffer = ByteBuffer.allocate(buffer.capacity());

        final SSLEngineResult result = sslEngine.unwrap(buffer, unwrapBuffer);

        switch (result.getStatus()) {
            case OK:
                unwrapBuffer.flip();
                return unwrapBuffer;
            case BUFFER_OVERFLOW:
                return SslSupport.enlargeApplicationBuffer(sslEngine, unwrapBuffer);
            case BUFFER_UNDERFLOW:
                return SslSupport.handleBufferUnderflow(sslEngine, buffer);
            case CLOSED:
                System.out.println("Closing SSL connection...");
                SslSupport.closeConnection(channel, sslEngine);
                return null;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
        }
    }

}
