import com.airepublic.microprofile.core.IServerModule;

module com.airepublic.microprofile.core {
    exports com.airepublic.microprofile.core;
    exports com.airepublic.microprofile.core.pathmatcher;

    requires com.airepublic.microprofile.config;

    requires cdi.api;
    requires java.annotation;
    requires transitive java.persistence;
    requires javax.inject;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires openwebbeans.se;
    requires openwebbeans.spi;
    requires openwebbeans.impl;
    // requires weld.se.core;

    requires java.net.http;

    opens com.airepublic.microprofile.core;

    uses IServerModule;

}