package com.airepublic.microprofile.core;

import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;

public class SessionContainer {
    private static AtomicLong SESSION_ID_GENERATOR = new AtomicLong();
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private IServerModule module;
    private SocketChannel channel;
    @Inject
    private ServerContext serverContext;
    @Inject
    private SessionScopedContext sessionScopedContext;


    public void init(final IServerModule module, final SocketChannel channel) {
        this.module = module;
        this.channel = channel;
    }


    @Asynchronous
    public Future<Void> run() {
        ServerSession session = null;
        final long sessionId = SESSION_ID_GENERATOR.incrementAndGet();
        final SessionContext sessionContext = new SessionContext(sessionId);

        try {
            logger.info("Starting session #" + sessionId + " for module " + module.getName());

            sessionScopedContext.activate(sessionContext);

            session = serverContext.getCdiContainer().select(ServerSession.class).get();
            session.init(sessionId, module, channel, serverContext);
            session.run();
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error creating session", e);
        } finally {
            logger.info("Closing session #" + sessionId + " for module '" + module.getName() + "'!");
            sessionScopedContext.deactivate();
            logger.info("Session #" + sessionId + " for module '" + module.getName() + "' destroyed!");
        }

        return CompletableFuture.completedFuture(null);
    }
}
