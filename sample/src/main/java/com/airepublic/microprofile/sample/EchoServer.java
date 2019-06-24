package com.airepublic.microprofile.sample;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/ws")
public class EchoServer {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServer.class);
    int id = 0;


    @OnOpen
    public void onOpen(final Session session) {
        LOG.info("Connected ... " + session.getId());
    }


    @OnMessage
    public String onMessage(final String message, final Session session) {
        System.out.println("onMessage: " + message);
        switch (message) {
            case "quit":
                try {
                    session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Game ended"));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            break;
        }
        id++;
        return String.format("%d:%s", id, message);
    }


    @OnMessage
    public void onMessage(final byte[] bytes, final Session session) {
        try {
            System.out.println("onMessage (b): " + new String(bytes, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @OnError
    public void onError(final Session session, final Throwable t) {
        t.printStackTrace();
    }


    @OnClose
    public void onClose(final Session session, final CloseReason closeReason) {
        System.out.println("Closed: " + session.getId() + " -> " + closeReason);
    }
}