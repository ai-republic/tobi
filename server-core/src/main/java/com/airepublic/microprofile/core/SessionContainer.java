package com.airepublic.microprofile.core;

import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.SessionAttributes;
import com.airepublic.microprofile.core.spi.SessionContext;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

public class SessionContainer {
    private static AtomicLong SESSION_ID_GENERATOR = new AtomicLong();
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private RequestScopedContext requestScopedContext;
    @Inject
    private SessionScopedContext sessionScopedContext;


    @Asynchronous
    public Future<ServerSession> startSession(final IServerModule module, final Supplier<SocketChannel> channelSupplier, final SessionAttributes sessionAttributes, final boolean isClient) {
        ServerSession session = null;
        final long sessionId = SESSION_ID_GENERATOR.incrementAndGet();

        try {
            logger.info("Starting session #" + sessionId + " for module " + module.getName());

            requestScopedContext.activate(new RequestContext(sessionId));
            sessionScopedContext.activate(new SessionContext(sessionId));

            session = CDI.current().select(ServerSession.class).get();
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error creating session", e);
        } finally {
            sessionScopedContext.deactivate();
            requestScopedContext.deactivate();
            logger.info("Session #" + sessionId + " for module '" + module.getName() + "' destroyed!");
        }

        return CompletableFuture.completedFuture(session);
    }
}
