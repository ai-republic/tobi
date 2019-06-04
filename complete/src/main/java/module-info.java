module com.airepublic.microprofile.complete {
    exports com.airepublic.microprofile.javaserver.boot;
    exports com.airepublic.microprofile.sample;

    requires com.airepublic.microprofile.config;
    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.module.http;
    requires transitive com.airepublic.microprofile.module.http.jaxrs.resteasy;
    requires transitive com.airepublic.microprofile.module.http.websocket;

    requires cdi.api;
    requires java.annotation;
    requires transitive java.persistence;
    requires transitive java.ws.rs;
    requires javax.inject;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires openwebbeans.se;
    requires openwebbeans.spi;
    requires openwebbeans.impl;
    // requires weld.se.core;

    requires java.xml.bind;
    requires java.net.http;

    // requires transitive resteasy.jaxb.provider;
    requires transitive resteasy.jaxrs;

    requires org.eclipse.jetty.websocket.api;
    requires org.eclipse.jetty.websocket.client;
    requires org.eclipse.jetty.websocket.common;
    requires org.eclipse.jetty.util;

    opens com.airepublic.microprofile.client;

}