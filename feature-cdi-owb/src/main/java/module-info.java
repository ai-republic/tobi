import com.airepublic.tobi.core.spi.ICDIServiceProvider;
import com.airepublic.tobi.feature.cdi.owb.CDIServiceProviderImpl;

module com.airepublic.tobi.feature.cdi.owb {
    exports com.airepublic.tobi.feature.cdi.owb;

    requires com.airepublic.tobi.core.spi;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires transitive java.annotation;

    requires transitive openwebbeans.se;
    requires transitive openwebbeans.spi;
    requires transitive openwebbeans.impl;

    provides ICDIServiceProvider with CDIServiceProviderImpl;

}