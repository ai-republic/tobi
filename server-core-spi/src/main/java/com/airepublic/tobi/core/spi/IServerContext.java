package com.airepublic.tobi.core.spi;

import java.io.IOException;
import java.net.SocketAddress;

public interface IServerContext {

    IServerContext setAttribute(String key, Object value);


    Object getAttribute(String key);


    boolean hasAttribute(String key);


    String getHost();


    int getWorkerCount();


    void addServerSession(final SocketAddress remoteAddress, final IServerSession session);


    IServerSession getServerSession(final SocketAddress remoteAddress);


    void removeServerSession(final IServerSession session) throws IOException;


    SessionContext getSessionContext(String sessionId);


    void addSessionContext(String sessionId, SessionContext sessionContext);


    void removeSessionContext(String sessionId);

}