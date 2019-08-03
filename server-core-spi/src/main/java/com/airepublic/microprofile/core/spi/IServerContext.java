package com.airepublic.microprofile.core.spi;

public interface IServerContext {

    IServerContext setAttribute(String key, Object value);


    Object getAttribute(String key);


    boolean hasAttribute(String key);


    String getHost();


    int getWorkerCount();


    SessionContext getSessionContext(Long sessionId);


    void addSessionContext(Long sessionId, SessionContext sessionContext);


    void removeSessionContext(Long sessionId);

}