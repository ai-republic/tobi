package com.airepublic.microprofile.sample;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

@OpenAPIDefinition(info = @Info(title = "EchoServer", contact = @Contact(name = "Torsten Oltmanns"), version = "1.0"))
@ServerEndpoint(value = "/ws")
public class EchoServer {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    int id = 0;


    @Timed(description = "Echoserver open time", tags = "type=Websocket")
    @OnOpen
    public void onOpen(final Session session) {
        logger.info("Connected ... " + session.getId());
    }


    @Timeout
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


    @CircuitBreaker(delay = 100)
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