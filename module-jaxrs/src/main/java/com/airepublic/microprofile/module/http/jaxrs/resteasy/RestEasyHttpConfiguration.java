package com.airepublic.microprofile.module.http.jaxrs.resteasy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.resteasy.spi.ResteasyConfiguration;

public class RestEasyHttpConfiguration implements ResteasyConfiguration {
    private final Map<String, String> config = new HashMap<>();


    @Override
    public String getParameter(final String name) {
        return config.get(name);
    }


    @Override
    public Set<String> getParameterNames() {
        return config.keySet();
    }


    @Override
    public String getInitParameter(final String name) {
        return getParameter(name);
    }


    @Override
    public Set<String> getInitParameterNames() {
        return getParameterNames();
    }

}
