module com.airepublic.microprofile.module.http.core {
    exports com.airepublic.microprofile.module.http.core;

    requires com.airepublic.microprofile.config;
    requires transitive com.airepublic.microprofile.core;

    opens com.airepublic.microprofile.module.http.core;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.buf;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.codec.binary;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.security;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.util.threads;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.websocket;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.websocket.pojo;
    // opens com.airepublic.microprofile.javaserver.websocket.tomcat.websocket.server;
}