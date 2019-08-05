import com.airepublic.tobi.core.spi.IServicePlugin;
import com.airepublic.tobi.plugin.http.sse.SsePlugin;

module com.airepublic.tobi.plugin.http.sse {
    exports com.airepublic.tobi.plugin.http.sse;

    requires transitive com.airepublic.tobi.core.spi;
    requires com.airepublic.tobi.module.http;
    requires com.airepublic.tobi.feature.mp.config;
    requires transitive com.airepublic.http.sse.api;
    requires transitive com.airepublic.http.sse.impl;
    requires com.airepublic.logging.java;
    requires com.airepublic.reflections;
    requires com.airepublic.http.common;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires java.logging;
    requires microprofile.config.api;

    provides IServicePlugin with SsePlugin;

    opens com.airepublic.tobi.plugin.http.sse;

}