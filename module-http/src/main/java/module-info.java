import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.module.http.HttpModule;

module com.airepublic.microprofile.module.http {
    exports com.airepublic.microprofile.module.http;

    requires com.airepublic.microprofile.feature.mp.config;
    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.util.http.common;

    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires java.annotation;
    requires cdi.api;
    requires javax.inject;

    provides IServerModule with HttpModule;

    opens com.airepublic.microprofile.module.http;
}