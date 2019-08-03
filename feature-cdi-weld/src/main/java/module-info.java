module com.airepublic.microprofile.feature.cdi.weld {
    exports com.airepublic.microprofile.feature.cdi.weld;

    // requires com.airepublic.microprofile.core.spi;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires transitive weld.api;
    requires transitive weld.environment.common;
    requires transitive weld.se.core;
    requires transitive weld.core.impl;
    requires transitive jdk.unsupported;

    // provides ICDIServiceProvider with CDIServiceProviderImpl;

    opens com.airepublic.microprofile.feature.cdi.weld;
}