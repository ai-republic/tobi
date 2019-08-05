import javax.websocket.server.ServerEndpointConfig;

import com.airepublic.tobi.core.spi.IServicePlugin;
import com.airepublic.tobi.plugin.http.websocket.WebSocketPlugin;
import com.airepublic.tobi.plugin.http.websocket.server.DefaultServerEndpointConfigurator;

module com.airepublic.tobi.plugin.http.websocket {
    exports com.airepublic.tobi.plugin.http.websocket;

    exports com.airepublic.tobi.plugin.http.websocket.server;
    exports com.airepublic.tobi.plugin.http.websocket.util;
    exports com.airepublic.tobi.plugin.http.websocket.util.buf;
    exports com.airepublic.tobi.plugin.http.websocket.util.collections;
    exports com.airepublic.tobi.plugin.http.websocket.util.threads;

    opens com.airepublic.tobi.plugin.http.websocket.pojo;
    opens com.airepublic.tobi.plugin.http.websocket.server;
    opens com.airepublic.tobi.plugin.http.websocket.util.buf;
    opens com.airepublic.tobi.plugin.http.websocket.util.codec.binary;
    opens com.airepublic.tobi.plugin.http.websocket.util.security;
    opens com.airepublic.tobi.plugin.http.websocket.util.threads;

    requires com.airepublic.tobi.core.spi;
    requires com.airepublic.tobi.module.http;
    requires com.airepublic.tobi.feature.mp.config;
    requires com.airepublic.logging.java;
    requires com.airepublic.reflections;
    requires com.airepublic.http.common;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires transitive javax.websocket.api;

    opens com.airepublic.tobi.plugin.http.websocket;

    provides ServerEndpointConfig.Configurator with DefaultServerEndpointConfigurator;

    provides IServicePlugin with WebSocketPlugin;

}