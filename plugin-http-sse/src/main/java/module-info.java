import com.airepublic.microprofile.core.spi.IServicePlugin;
import com.airepublic.microprofile.plugin.http.sse.SsePlugin;

module com.airepublic.microprofile.plugin.http.sse {
    exports com.airepublic.microprofile.plugin.http.sse;
    exports com.airepublic.microprofile.plugin.http.sse.api;

    requires com.airepublic.microprofile.core;
    requires com.airepublic.microprofile.core.spi;
    requires com.airepublic.microprofile.feature.logging.java;
    requires com.airepublic.microprofile.feature.mp.config;
    requires com.airepublic.microprofile.util.http.common;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires java.logging;
    requires microprofile.config.api;

    provides IServicePlugin with SsePlugin;

    opens com.airepublic.microprofile.plugin.http.sse;
    opens com.airepublic.microprofile.plugin.http.sse.api;

}