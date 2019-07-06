import com.airepublic.microprofile.core.spi.ICDIServiceProvider;
import com.airepublic.microprofile.feature.cdi.owb.CDIServiceProviderImpl;

module com.airepublic.microprofile.feature.cdi.owb {
    exports com.airepublic.microprofile.feature.cdi.owb;

    requires com.airepublic.microprofile.core.spi;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires transitive java.annotation;

    requires transitive openwebbeans.se;
    requires transitive openwebbeans.spi;
    requires transitive openwebbeans.impl;

    provides ICDIServiceProvider with CDIServiceProviderImpl;

}