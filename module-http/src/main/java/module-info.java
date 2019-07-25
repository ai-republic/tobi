import com.airepublic.microprofile.core.spi.IChannelEncoder;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.module.http.HttpChannelEncoder;
import com.airepublic.microprofile.module.http.HttpModule;

module com.airepublic.microprofile.module.http {
    exports com.airepublic.microprofile.module.http;

    requires com.airepublic.microprofile.feature.mp.config;
    requires transitive com.airepublic.microprofile.core.spi;
    requires transitive com.airepublic.microprofile.util.http.common;
    requires com.airepublic.microprofile.feature.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    provides IServerModule with HttpModule;
    provides IChannelEncoder with HttpChannelEncoder;

    opens com.airepublic.microprofile.module.http;
}