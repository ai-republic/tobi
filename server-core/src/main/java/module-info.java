import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.core.IServicePlugin;
import com.airepublic.microprofile.core.spi.ICDIServiceProvider;

module com.airepublic.microprofile.core {
    exports com.airepublic.microprofile.core;
    exports com.airepublic.microprofile.core.pathmatcher;
    exports com.airepublic.microprofile.core.util;

    requires com.airepublic.microprofile.core.spi;
    requires com.airepublic.microprofile.feature.mp.config;
    requires com.airepublic.microprofile.feature.mp.faulttolerance;
    requires com.airepublic.microprofile.feature.logging.java;

    requires java.annotation;
    requires cdi.api;
    requires javax.inject;
    requires javax.interceptor.api;

    requires transitive java.net.http;

    opens com.airepublic.microprofile.core;

    uses IServerModule;
    uses IServicePlugin;
    uses ICDIServiceProvider;
}