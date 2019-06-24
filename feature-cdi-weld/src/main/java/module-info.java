import com.airepublic.microprofile.core.spi.ICDIServiceProvider;
import com.airepublic.microprofile.feature.cdi.weld.CDIServiceProviderImpl;

module com.airepublic.microprofile.feature.cdi.weld {
    exports com.airepublic.microprofile.feature.cdi.weld;

    requires com.airepublic.microprofile.core.spi;

    requires java.annotation;
    requires cdi.api;
    requires javax.inject;

    requires transitive weld.api;
    requires transitive weld.environment.common;
    requires transitive weld.se.core;
    requires transitive weld.core.impl;
    requires transitive jdk.unsupported;

    provides ICDIServiceProvider with CDIServiceProviderImpl;

    opens com.airepublic.microprofile.feature.cdi.weld;
}