import com.airepublic.microprofile.core.IServicePlugin;
import com.airepublic.microprofile.plugin.http.jaxrs.resteasy.RestEasyPlugin;

module com.airepublic.microprofile.plugin.http.jaxrs.resteasy {
    exports com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.util.http.common;
    requires com.airepublic.microprofile.feature.mp.config;
    requires com.airepublic.microprofile.feature.logging.java;

    requires cdi.api;
    requires javax.inject;
    requires java.annotation;

    requires java.xml.bind;

    // requires transitive resteasy.jaxb.provider;
    requires transitive java.ws.rs;
    requires transitive resteasy.jaxrs;

    provides IServicePlugin with RestEasyPlugin;

    opens com.airepublic.microprofile.plugin.http.jaxrs.resteasy;
}