package com.airepublic.microprofile.plugin.http.sse.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.airepublic.microprofile.util.http.common.BufferUtil;

public class TestSseService {
    final SseService service = new SseService();


    protected SocketChannel openChannel(final URI uri) {
        final boolean isSecure = uri.getScheme().toLowerCase().equals("https");
        int port = uri.getPort();

        if (port <= 0) {
            port = isSecure ? 443 : 80;
        }

        SocketChannel channel;

        try {
            final SocketAddress remote = new InetSocketAddress(uri.getHost(), port);
            channel = SocketChannel.open();
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            channel.configureBlocking(false);
            channel.connect(remote);

            while (!channel.finishConnect()) {
                Thread.onSpinWait();
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create socket channel!", e);
        }

        return channel;
    }


    @Test
    public void testReceive() throws IOException, URISyntaxException {
        final AtomicBoolean called = new AtomicBoolean(false);
        final Future<Void> future = service.receive(new URI("https://api.boerse-frankfurt.de:443/data/price_information?isin=US00724F1012&mic=XFRA"), e -> called.set(true));
        try {
            Thread.sleep(30000);
            future.cancel(true);
        } catch (InterruptedException | CancellationException e) {
            e.printStackTrace();
        }

        Assertions.assertTrue(called.get());
    }


    @Test
    public void testSend() throws Exception {
        // GIVEN:
        // - a server exists which performs SSE handshake and replies with an event
        final SseEvent event = new SseEvent.Builder()
                .withId("1")
                .withName("Test")
                .withComment("a comment")
                .withRetry(1)
                .withData("Hello world").build();

        final SimpleServer server = new SimpleServer(8888, serverChannel -> {
            try {
                service.handshake(serverChannel, null);
                service.send(event, serverChannel, null);
                service.send(event, serverChannel, null);
            } catch (final Exception e) {
                Assertions.fail(e);
            }
        });

        new Thread(server, "SimpleServer").start();

        // WHEN:
        // - a client connects to the SSE server
        try {

            final SocketChannel client = openChannel(new URI("http://localhost:8888"));

            // wait for the connect and accept and send
            Thread.sleep(1500);

            // THEN:
            // - the client should receive the exact event
            final ByteBuffer buffer = ByteBuffer.allocate(4096);
            client.read(buffer);
            buffer.flip();

            // buffer.mark();
            // final byte[] bytes = new byte[buffer.remaining()];
            // buffer.get(bytes);
            // buffer.reset();
            // System.out.println(new String(bytes, "UTF-8"));

            // skip header
            while (!BufferUtil.readLine(buffer, StandardCharsets.UTF_8).isBlank()) {
            }

            final List<SseEvent> result = service.decode(buffer, false);

            for (final SseEvent received : result) {
                Assertions.assertEquals(event.getName(), received.getName());
                Assertions.assertEquals(event.getId(), received.getId());
                Assertions.assertEquals(event.getComment(), received.getComment());
                Assertions.assertEquals(event.getRetry(), received.getRetry());
                Assertions.assertEquals(event.getData(), received.getData());
            }
        } finally {
            server.close();
        }

    }


    public static void main(final String[] args) throws IOException, URISyntaxException {
        final TestSseService test = new TestSseService();
        test.testReceive();
    }
}
