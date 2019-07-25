package com.airepublic.microprofile.core;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.se.SeContainer;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.SessionContext;

@ApplicationScoped
public class ServerContext implements IServerContext {
    public final static String HOST = "host";
    public final static String WORKER_COUNT = "workerCount";
    private final static String DEFAULT_WORKER_COUNT = "10";
    @Inject
    @ConfigProperty(name = HOST, defaultValue = "localhost")
    private String host;
    @Inject
    @ConfigProperty(name = WORKER_COUNT, defaultValue = DEFAULT_WORKER_COUNT)
    private int workerCount;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<SocketAddress, IServerSession> openSessions = new ConcurrentHashMap<>();
    private final Map<Long, SessionContext> sessionContexts = new ConcurrentHashMap<>();
    private SeContainer cdiContainer;
    private final Set<IServerModule> modules = new HashSet<>();


    public ServerContext setAttribute(final String key, final Object value) {
        attributes.put(key, value);
        return this;
    }


    public Object getAttribute(final String key) {
        return attributes.get(key);
    }


    public boolean hasAttribute(final String key) {
        return attributes.containsKey(key);
    }


    ServerContext addModule(final IServerModule module) {
        if (!modules.contains(module)) {
            modules.add(module);
        }

        return this;
    }


    Set<IServerModule> getModules() {
        return Collections.unmodifiableSet(modules);
    }


    void removeModule(final IServerModule module) {
        modules.remove(module);
    }


    public String getHost() {
        return host;
    }


    ServerContext setHost(final String host) {
        this.host = host;
        return this;
    }


    public int getWorkerCount() {
        return workerCount;
    }


    ServerContext setWorkerCount(final int workerCount) {
        this.workerCount = workerCount;
        return this;
    }


    void addServerSession(final SocketAddress remoteAddress, final IServerSession session) {
        openSessions.put(remoteAddress, session);
    }


    void removeServerSession(final IServerSession session) throws IOException {
        openSessions.remove(session.getChannel().getRemoteAddress());
    }


    public IServerSession getServerSession(final SocketAddress remoteAddress) {
        return openSessions.get(remoteAddress);
    }


    Collection<IServerSession> getOpenServerSessions() {
        return Collections.unmodifiableCollection(openSessions.values());
    }


    public void addSessionContext(final Long sessionId, final SessionContext sessionContext) {
        sessionContexts.put(sessionId, sessionContext);
    }


    public SessionContext getSessionContext(final Long sessionId) {
        return sessionContexts.get(sessionId);
    }


    public void removeSessionContext(final Long sessionId) {
        sessionContexts.remove(sessionId);
    }


    public SeContainer getCdiContainer() {
        return cdiContainer;
    }


    ServerContext setCdiContainer(final SeContainer cdiContainer) {
        this.cdiContainer = cdiContainer;
        return this;
    }

}
