package com.airepublic.microprofile.core;

import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionContainer {
    private final static Logger LOG = LoggerFactory.getLogger(SessionContainer.class);
    private static AtomicLong SESSION_ID_GENERATOR = new AtomicLong();

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
            LOG.info("Starting session #" + sessionId + " for module " + module.getName());

            sessionScopedContext.activate(sessionContext);

            session = serverContext.getCdiContainer().select(ServerSession.class).get();
            session.init(sessionId, module, channel, serverContext);
            session.run();
        } catch (final Exception e) {
            LOG.error("Error creating session", e);
        } finally {
            LOG.info("Closing session #" + sessionId + " for module '" + module.getName() + "'!");
            sessionScopedContext.deactivate();
            LOG.info("Session #" + sessionId + " for module '" + module.getName() + "' destroyed!");
        }

        return CompletableFuture.completedFuture(null);
    }
}
