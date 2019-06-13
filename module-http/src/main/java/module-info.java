import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.module.http.HttpModule;

module com.airepublic.microprofile.module.http {
    exports com.airepublic.microprofile.module.http;
    exports com.airepublic.microprofile.module.http.handler;

    requires com.airepublic.microprofile.config;
    requires transitive com.airepublic.microprofile.core;

    provides IServerModule with HttpModule;

    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.buf;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.codec.binary;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.security;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.threads;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.websocket;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.websocket.pojo;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.websocket.server;
}