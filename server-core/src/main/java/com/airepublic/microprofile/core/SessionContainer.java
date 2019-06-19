package com.airepublic.microprofile.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.weld.context.bound.BoundSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionContainer implements Runnable {
    private final static Logger LOG = LoggerFactory.getLogger(SessionContainer.class);
    @Inject
    private BoundSessionContext sessionContext;
    private IServerModule module;
    private SocketChannel channel;
    @Inject
    private ServerContext serverContext;


    public void init(final IServerModule module, final SocketChannel channel) {
        this.module = module;
        this.channel = channel;
    }


    @Override
    public void run() {
        final Map<String, Object> storage = new HashMap<>();

        try {
            sessionContext.associate(storage);
            sessionContext.activate();

            final ServerSession session = serverContext.getCdiContainer().select(ServerSession.class).get();
            session.init(module, channel);
            session.run();
        } catch (final IOException e) {
            LOG.error("Error creating session", e);
        } finally {
            try {
                sessionContext.invalidate();
                sessionContext.deactivate();
            } finally {
                sessionContext.dissociate(storage);
            }
        }
    }
}
