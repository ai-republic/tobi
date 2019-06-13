import javax.websocket.server.ServerEndpointConfig;

import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.module.http.websocket.WebSocketModule;
import com.airepublic.microprofile.module.http.websocket.server.DefaultServerEndpointConfigurator;

module com.airepublic.microprofile.module.http.websocket {
    exports com.airepublic.microprofile.module.http.websocket;

    exports com.airepublic.microprofile.module.http.websocket.server;
    exports com.airepublic.microprofile.module.http.websocket.util;
    exports com.airepublic.microprofile.module.http.websocket.util.buf;
    exports com.airepublic.microprofile.module.http.websocket.util.collections;
    exports com.airepublic.microprofile.module.http.websocket.util.threads;

    opens com.airepublic.microprofile.module.http.websocket.pojo;
    opens com.airepublic.microprofile.module.http.websocket.server;
    opens com.airepublic.microprofile.module.http.websocket.util.buf;
    opens com.airepublic.microprofile.module.http.websocket.util.codec.binary;
    opens com.airepublic.microprofile.module.http.websocket.util.security;
    opens com.airepublic.microprofile.module.http.websocket.util.threads;

    requires com.airepublic.microprofile.core;
    requires com.airepublic.microprofile.config;
    requires com.airepublic.microprofile.module.http;

    requires javax.websocket.api;

    opens com.airepublic.microprofile.module.http.websocket;

    provides ServerEndpointConfig.Configurator with DefaultServerEndpointConfigurator;

    provides IServerModule with WebSocketModule;

}