module com.airepublic.microprofile.complete {
    exports com.airepublic.microprofile.javaserver.boot;
    exports com.airepublic.microprofile.sample;

    requires transitive com.airepublic.microprofile.config;
    requires com.airepublic.microprofile.jwtauth;
    requires transitive com.airepublic.microprofile.core;
    requires transitive com.airepublic.microprofile.module.http;
    requires transitive com.airepublic.microprofile.module.http.jaxrs.resteasy;
    requires transitive com.airepublic.microprofile.module.http.websocket;

    requires org.eclipse.jetty.websocket.api;
    requires org.eclipse.jetty.websocket.client;
    requires org.eclipse.jetty.websocket.common;
    requires org.eclipse.jetty.util;

    opens com.airepublic.microprofile.client;
}