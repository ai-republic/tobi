import javax.websocket.server.ServerEndpointConfig;

import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.module.http.websocket.WebSocketModule;
import com.airepublic.microprofile.module.http.websocket.server.DefaultServerEndpointConfigurator;

module com.airepublic.microprofile.module.http.websocket {
    exports com.airepublic.microprofile.module.http.websocket;

    requires com.airepublic.microprofile.core;
    requires com.airepublic.microprofile.config;
    requires com.airepublic.microprofile.module.http;

    requires cdi.api;
    requires java.annotation;
    requires javax.inject;
    requires javax.websocket.api;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires openwebbeans.se;
    requires openwebbeans.spi;
    requires openwebbeans.impl;
    // requires weld.se.core;

    requires java.net.http;

    opens com.airepublic.microprofile.module.http.websocket;

    provides ServerEndpointConfig.Configurator with DefaultServerEndpointConfigurator;

    provides IServerModule with WebSocketModule;

}