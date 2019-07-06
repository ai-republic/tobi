package com.airepublic.microprofile.core.spi;

import javax.enterprise.inject.se.SeContainer;

public interface IServerContext {

    IServerContext setAttribute(String key, Object value);


    Object getAttribute(String key);


    boolean hasAttribute(String key);


    String getHost();


    int getWorkerCount();


    SeContainer getCdiContainer();

}