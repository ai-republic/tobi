import javax.websocket.server.ServerEndpointConfig;

import com.airepublic.microprofile.core.IServicePlugin;
import com.airepublic.microprofile.plugin.http.websocket.WebSocketPlugin;
import com.airepublic.microprofile.plugin.http.websocket.server.DefaultServerEndpointConfigurator;

module com.airepublic.microprofile.plugin.http.websocket {
    exports com.airepublic.microprofile.plugin.http.websocket;

    exports com.airepublic.microprofile.plugin.http.websocket.server;
    exports com.airepublic.microprofile.plugin.http.websocket.util;
    exports com.airepublic.microprofile.plugin.http.websocket.util.buf;
    exports com.airepublic.microprofile.plugin.http.websocket.util.collections;
    exports com.airepublic.microprofile.plugin.http.websocket.util.threads;

    opens com.airepublic.microprofile.plugin.http.websocket.pojo;
    opens com.airepublic.microprofile.plugin.http.websocket.server;
    opens com.airepublic.microprofile.plugin.http.websocket.util.buf;
    opens com.airepublic.microprofile.plugin.http.websocket.util.codec.binary;
    opens com.airepublic.microprofile.plugin.http.websocket.util.security;
    opens com.airepublic.microprofile.plugin.http.websocket.util.threads;

    requires com.airepublic.microprofile.core;
    requires com.airepublic.microprofile.feature.logging.java;
    requires com.airepublic.microprofile.feature.mp.config;
    requires com.airepublic.microprofile.util.http.common;

    requires cdi.api;
    requires javax.inject;
    requires java.annotation;

    requires transitive javax.websocket.api;

    opens com.airepublic.microprofile.plugin.http.websocket;

    provides ServerEndpointConfig.Configurator with DefaultServerEndpointConfigurator;

    provides IServicePlugin with WebSocketPlugin;

}