package com.airepublic.microprofile.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.inject.se.SeContainer;
import javax.inject.Singleton;

@Singleton
public class ServerContext {
    private String host;
    private int port;
    private int sslPort;
    private int workerCount;
    private String keystorePassword;
    private String truststorePassword;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Queue<ServerSession> openSessions = new ConcurrentLinkedQueue<>();
    private SeContainer cdiContainer;
    private final Set<IServerModule> modules = new HashSet<>();


    public static ServerContext create(final String host, final int port, final int sslPort) {
        return new ServerContext(host, port, sslPort);
    }


    private ServerContext(final String host, final int defaultPort, final int defaultSslPort) {
        this.host = host;
        port = defaultPort;
        sslPort = defaultSslPort;
    }


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


    public int getPort() {
        return port;
    }


    ServerContext setPort(final int port) {
        this.port = port;
        return this;
    }


    public int getSslPort() {
        return sslPort;
    }


    ServerContext setSslPort(final int sslPort) {
        this.sslPort = sslPort;
        return this;
    }


    public int getWorkerCount() {
        return workerCount;
    }


    ServerContext setWorkerCount(final int workerCount) {
        this.workerCount = workerCount;
        return this;
    }


    String getKeystorePassword() {
        return keystorePassword;
    }


    ServerContext setKeystorePassword(final String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }


    String getTruststorePassword() {
        return truststorePassword;
    }


    ServerContext setTruststorePassword(final String truststorePassword) {
        this.truststorePassword = truststorePassword;
        return this;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (attributes == null ? 0 : attributes.hashCode());
        result = prime * result + (host == null ? 0 : host.hashCode());
        result = prime * result + port;
        return result;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ServerContext other = (ServerContext) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "ServerContext [port=" + port + ", host=" + host + ", attributes=" + attributes + "]";
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
