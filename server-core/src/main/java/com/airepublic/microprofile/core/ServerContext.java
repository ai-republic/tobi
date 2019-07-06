package com.airepublic.microprofile.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.se.SeContainer;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;

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
    private final Queue<ServerSession> openSessions = new ConcurrentLinkedQueue<>();
    private SeContainer cdiContainer;
    private final Set<IServerModule> modules = new HashSet<>();


    @Override
    public IServerContext setAttribute(final String key, final Object value) {
        attributes.put(key, value);
        return this;
    }


    @Override
    public Object getAttribute(final String key) {
        return attributes.get(key);
    }


    @Override
    public boolean hasAttribute(final String key) {
        return attributes.containsKey(key);
    }


    IServerContext addModule(final IServerModule module) {
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


    @Override
    public String getHost() {
        return host;
    }


    IServerContext setHost(final String host) {
        this.host = host;
        return this;
    }


    @Override
    public int getWorkerCount() {
        return workerCount;
    }


    IServerContext setWorkerCount(final int workerCount) {
        this.workerCount = workerCount;
        return this;
    }


    void addServerSession(final ServerSession session) {
        openSessions.add(session);
    }


    void removeServerSession(final IServerSession session) {
        openSessions.remove(session);
    }


    Queue<ServerSession> getOpenServerSessions() {
        return openSessions;
    }


    @Override
    public SeContainer getCdiContainer() {
        return cdiContainer;
    }


    IServerContext setCdiContainer(final SeContainer cdiContainer) {
        this.cdiContainer = cdiContainer;
        return this;
    }
}
