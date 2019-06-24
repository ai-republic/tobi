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

@ApplicationScoped
public class ServerContext {
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


    void addServerSession(final ServerSession session) {
        openSessions.add(session);
    }


    void removeServerSession(final ServerSession session) {
        openSessions.remove(session);
    }


    Queue<ServerSession> getOpenServerSessions() {
        return openSessions;
    }


    public SeContainer getCdiContainer() {
        return cdiContainer;
    }


    ServerContext setCdiContainer(final SeContainer cdiContainer) {
        this.cdiContainer = cdiContainer;
        return this;
    }
}
