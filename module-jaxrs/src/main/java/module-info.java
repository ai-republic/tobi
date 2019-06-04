import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.module.http.jaxrs.resteasy.RestEasyModule;

module com.airepublic.microprofile.module.http.jaxrs.resteasy {
    exports com.airepublic.microprofile.module.http.jaxrs.resteasy;

    requires com.airepublic.microprofile.config;
    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.module.http;

    requires jdk.unsupported;
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

    provides IServerModule with RestEasyModule;
}