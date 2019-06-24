module com.airepublic.microprofile.samplw {
    exports com.airepublic.microprofile.javaserver.boot;
    exports com.airepublic.microprofile.sample;

    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.module.http;
    requires transitive com.airepublic.microprofile.plugin.http.jaxrs.resteasy;
    requires transitive com.airepublic.microprofile.plugin.http.websocket;
    requires transitive com.airepublic.microprofile.feature.cdi.weld;
    requires transitive com.airepublic.microprofile.feature.mp.config;
    requires transitive com.airepublic.microprofile.feature.mp.faulttolerance;
    requires transitive com.airepublic.microprofile.feature.mp.health;
    requires transitive com.airepublic.microprofile.feature.mp.jwtauth;
    requires transitive com.airepublic.microprofile.feature.mp.metrics;
    requires transitive com.airepublic.microprofile.feature.mp.openapi;
    requires transitive com.airepublic.microprofile.feature.mp.opentracing;
    requires transitive com.airepublic.microprofile.feature.mp.restclient;

    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires cdi.api;
    requires java.annotation;
    requires javax.inject;

    requires org.eclipse.jetty.websocket.api;
    requires org.eclipse.jetty.websocket.client;
    requires org.eclipse.jetty.websocket.common;
    requires org.eclipse.jetty.util;

    opens com.airepublic.microprofile.sample;
    opens com.airepublic.microprofile.client;
}