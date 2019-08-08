package com.airepublic.tobi.core;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.airepublic.tobi.core.spi.Attributes;
import com.airepublic.tobi.core.spi.BeanContextStorage;
import com.airepublic.tobi.core.spi.IServerContext;
import com.airepublic.tobi.core.spi.IServerSession;

/**
 * Context information of the server such as host, open sessions and other application attributes.
 * 
 * @author Torsten Oltmanns
 *
 */
@ApplicationScoped
public class ServerContext extends Attributes implements IServerContext {
    public final static String HOST = "host";
    public final static String WORKER_COUNT = "workerCount";
    private final static String DEFAULT_WORKER_COUNT = "10";
    @Inject
    @ConfigProperty(name = HOST, defaultValue = "localhost")
    private String host;
    @Inject
    @ConfigProperty(name = WORKER_COUNT, defaultValue = DEFAULT_WORKER_COUNT)
    private int workerCount;
    private final Map<SocketAddress, IServerSession> openSessions = new ConcurrentHashMap<>();
    private final Map<String, BeanContextStorage> sessionContexts = new ConcurrentHashMap<>();


    @Override
    public String getHost() {
        return host;
    }


    @Override
    public int getWorkerCount() {
        return workerCount;
    }


    @Override
    public void addServerSession(final SocketAddress remoteAddress, final IServerSession session) {
        openSessions.put(remoteAddress, session);
    }


    @Override
    public void removeServerSession(final IServerSession session) throws IOException {
        openSessions.remove(session.getChannel().getRemoteAddress());
    }


    @Override
    public IServerSession getServerSession(final SocketAddress remoteAddress) {
        return openSessions.get(remoteAddress);
    }


    @Override
    public void addSessionContext(final String sessionId, final BeanContextStorage sessionContext) {
        sessionContexts.put(sessionId, sessionContext);
    }


    @Override
    public BeanContextStorage getSessionContext(final String sessionId) {
        return sessionContexts.get(sessionId);
    }


    @Override
    public void removeSessionContext(final String sessionId) {
        sessionContexts.remove(sessionId);
    }

}
