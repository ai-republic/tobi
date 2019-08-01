import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.plugin.http.sse.SsePlugin;

module com.airepublic.microprofile.plugin.http.sse {
    exports com.airepublic.microprofile.plugin.http.sse;

    requires transitive com.airepublic.microprofile.core.spi;
    requires com.airepublic.microprofile.module.http;
    requires com.airepublic.microprofile.feature.logging.java;
    requires com.airepublic.microprofile.feature.mp.config;
    requires transitive com.airepublic.http.sse.api;
    requires transitive com.airepublic.http.sse.impl;
    requires com.airepublic.reflections;
    requires com.airepublic.http.common;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires java.logging;
    requires microprofile.config.api;

    provides IServicePlugin with SsePlugin;

    opens com.airepublic.microprofile.plugin.http.sse;

    requires weld.core.impl;
    requires jdk.unsupported;

}