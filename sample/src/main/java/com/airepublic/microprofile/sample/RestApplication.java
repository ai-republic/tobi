package com.airepublic.microprofile.sample;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/api")
public class RestApplication extends Application {
    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();


    public RestApplication() {
        classes.add(Service.class);
        classes.add(SseSample.class);
    }


    @Override
    public Set<java.lang.Class<?>> getClasses() {
        return classes;
    }


    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
