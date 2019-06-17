import com.airepublic.microprofile.core.IServicePlugin;
import com.airepublic.microprofile.module.http.jaxrs.resteasy.RestEasyPlugin;

module com.airepublic.microprofile.module.http.jaxrs.resteasy {
    exports com.airepublic.microprofile.module.http.jaxrs.resteasy;

    requires com.airepublic.microprofile.config;
    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.module.http.core;

    requires java.xml.bind;

    // requires transitive resteasy.jaxb.provider;
    requires transitive java.ws.rs;
    requires transitive resteasy.jaxrs;

    provides IServicePlugin with RestEasyPlugin;

    opens com.airepublic.microprofile.module.http.jaxrs.resteasy;
}