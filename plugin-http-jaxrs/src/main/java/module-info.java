import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.plugin.http.jaxrs.resteasy.RestEasyPlugin;

module com.airepublic.microprofile.plugin.http.jaxrs.resteasy {
    exports com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

    requires transitive com.airepublic.microprofile.core.spi;
    requires transitive com.airepublic.http.common;
    requires com.airepublic.microprofile.module.http;
    requires com.airepublic.microprofile.feature.mp.config;
    requires com.airepublic.microprofile.feature.logging.java;
    requires com.airepublic.reflections;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires transitive java.ws.rs;
    requires resteasy.jaxrs;
    requires resteasy.cdi;
    requires org.jboss.logging;
    requires java.naming;

    provides IServicePlugin with RestEasyPlugin;

    opens com.airepublic.microprofile.plugin.http.jaxrs.resteasy;
}