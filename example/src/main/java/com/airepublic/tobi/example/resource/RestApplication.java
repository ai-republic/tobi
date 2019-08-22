package com.airepublic.tobi.example.resource;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Example JAX-RS {@link Application}.
 * 
 * @author Torsten Oltmanns
 *
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();


    /**
     * Constructor.
     */
    public RestApplication() {
        classes.add(JaxRsSample.class);
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
