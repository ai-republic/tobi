import javax.ws.rs.ext.ContextResolver;

import com.airepublic.tobi.core.spi.IServicePlugin;
import com.airepublic.tobi.plugin.http.jaxrs.resteasy.ObjectMapperContextResolver;
import com.airepublic.tobi.plugin.http.jaxrs.resteasy.RestEasyPlugin;

module com.airepublic.tobi.plugin.http.jaxrs.resteasy {
    exports com.airepublic.tobi.plugin.http.jaxrs.resteasy;

    requires transitive com.airepublic.tobi.core.spi;
    requires transitive com.airepublic.http.common;
    requires com.airepublic.tobi.module.http;
    requires com.airepublic.tobi.feature.mp.config;
    requires com.airepublic.logging.java;
    requires com.airepublic.reflections;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires transitive java.ws.rs;
    requires resteasy.jaxrs;
    requires resteasy.cdi;
    requires resteasy.jackson2.provider;

    requires transitive com.fasterxml.jackson.annotation;
    requires transitive com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.datatype.jsr310;

    requires org.jboss.logging;
    requires java.naming;

    provides IServicePlugin with RestEasyPlugin;
    provides ContextResolver with ObjectMapperContextResolver;

    opens com.airepublic.tobi.plugin.http.jaxrs.resteasy;
}