package com.airepublic.tobi.client;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

public class WebSocketClient2 {
    @ClientEndpoint
    @WebSocket
    public static class WebSocketEndpoint {

        @OnOpen
        public void onOpen(final Session session, final EndpointConfig config) {
            System.out.println("Open: " + session);
        }


        @OnMessage
        @OnWebSocketMessage
        public void onMessage(final String msg) {
            System.out.println("Message: " + msg);
        }


        @OnMessage
        public void onBinary(final ByteBuffer bytes) {
            System.out.println("Message: " + bytes);
        }


        @OnWebSocketMessage
        public void onBinary(final byte[] bytes, final int offset, final int len) {
            System.out.println("Message: " + bytes);
        }


        @OnWebSocketClose
        public void onClose(final int code, final String closeReason) {
            System.out.println("Close: " + closeReason);
        }


        @OnError
        @OnWebSocketError
        public void onError(final Throwable ex) {
            ex.printStackTrace();
        }
    }


    public static void main(final String[] args) {
        final String destUri = "ws://localhost:8080/ws";
        final org.eclipse.jetty.websocket.client.WebSocketClient client = new org.eclipse.jetty.websocket.client.WebSocketClient();
        final WebSocketEndpoint socket = new WebSocketEndpoint();

        try {
            final URI echoUri = new URI(destUri);
            client.setAsyncWriteTimeout(299999);
            client.setMaxIdleTimeout(Integer.MAX_VALUE);
            client.setStopTimeout(Integer.MAX_VALUE);
            System.out.printf("Connecting to : %s%n", echoUri);
            client.start();
            final Future<org.eclipse.jetty.websocket.api.Session> session = client.connect(socket, echoUri);
            final org.eclipse.jetty.websocket.api.Session wsSession = session.get(30, TimeUnit.MINUTES);
            wsSession.getRemote().sendString("Hello");

            wsSession.getRemote().sendString("World");

            wsSession.getRemote().sendString("!!!");

            wsSession.getRemote().sendBytes(ByteBuffer.wrap("All send".getBytes()));

            wsSession.close(new CloseStatus(StatusCode.NORMAL, "Done"));

            client.stop();
        } catch (final Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                client.stop();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
}
