import javax.ws.rs.sse.Sse;

import com.airepublic.tobi.core.spi.IAuthenticationService;
import com.airepublic.tobi.example.authentication.jwt.JWTAuthenticationService;

module com.airepublic.tobi.sample {
    exports com.airepublic.tobi.example.authentication.jwt;
    exports com.airepublic.tobi.example.boot;
    exports com.airepublic.tobi.example.resource;

    requires transitive com.airepublic.tobi.core;
    requires transitive com.airepublic.tobi.core.spi;
    requires transitive com.airepublic.tobi.module.http;
    requires transitive com.airepublic.tobi.plugin.http.jaxrs.resteasy;
    requires transitive com.airepublic.tobi.plugin.http.websocket;
    requires transitive com.airepublic.tobi.plugin.http.sse;
    requires transitive com.airepublic.tobi.feature.cdi.weld;
    requires transitive com.airepublic.tobi.feature.mp.config;
    requires transitive com.airepublic.tobi.feature.mp.faulttolerance;
    requires transitive com.airepublic.tobi.feature.mp.health;
    requires transitive com.airepublic.tobi.feature.mp.jwtauth;
    requires transitive com.airepublic.tobi.feature.mp.metrics;
    requires transitive com.airepublic.tobi.feature.mp.openapi;
    requires transitive com.airepublic.tobi.feature.mp.opentracing;
    // requires transitive com.airepublic.tobi.feature.mp.restclient;
    requires transitive com.airepublic.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires org.eclipse.jetty.websocket.api;
    requires org.eclipse.jetty.websocket.client;
    requires org.eclipse.jetty.websocket.common;
    requires org.eclipse.jetty.util;

    requires java.ws.rs;
    requires resteasy.jaxrs;
    requires resteasy.cdi;
    requires java.activation;

    uses Sse;

    provides IAuthenticationService with JWTAuthenticationService;

    opens com.airepublic.tobi.example.authentication.jwt;
    opens com.airepublic.tobi.example.resource;
    opens com.airepublic.tobi.example.resource.websocket.client;

}