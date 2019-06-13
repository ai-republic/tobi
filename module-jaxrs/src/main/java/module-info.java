import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.module.http.jaxrs.resteasy.RestEasyModule;

module com.airepublic.microprofile.module.http.jaxrs.resteasy {
    exports com.airepublic.microprofile.module.http.jaxrs.resteasy;

    requires com.airepublic.microprofile.config;
    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.module.http;

    requires java.xml.bind;

    // requires transitive resteasy.jaxb.provider;
    requires transitive java.ws.rs;
    requires transitive resteasy.jaxrs;

    provides IServerModule with RestEasyModule;
}