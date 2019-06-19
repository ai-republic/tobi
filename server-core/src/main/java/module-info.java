import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.core.IServicePlugin;

module com.airepublic.microprofile.core {
    exports com.airepublic.microprofile.core;
    exports com.airepublic.microprofile.core.pathmatcher;
    exports com.airepublic.microprofile.core.util;

    requires transitive com.airepublic.microprofile.config;

    requires transitive org.slf4j;
    requires transitive ch.qos.logback.classic;

    requires jdk.unsupported;

    // requires cdi.api;
    // requires java.annotation;
    // requires javax.inject;
    // requires openwebbeans.se;
    // requires openwebbeans.spi;
    // requires openwebbeans.impl;
    requires transitive weld.se.shaded;

    requires transitive java.net.http;

    opens com.airepublic.microprofile.core;

    uses IServerModule;
    uses IServicePlugin;
}