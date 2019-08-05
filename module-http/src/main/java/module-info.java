import com.airepublic.tobi.core.spi.IChannelEncoder;
import com.airepublic.tobi.core.spi.IServerModule;
import com.airepublic.tobi.module.http.HttpChannelEncoder;
import com.airepublic.tobi.module.http.HttpModule;

module com.airepublic.tobi.module.http {
    exports com.airepublic.tobi.module.http;

    requires com.airepublic.tobi.feature.mp.config;
    requires transitive com.airepublic.tobi.core.spi;
    requires com.airepublic.http.common;
    requires com.airepublic.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    provides IServerModule with HttpModule;
    provides IChannelEncoder with HttpChannelEncoder;

    opens com.airepublic.tobi.module.http;
}