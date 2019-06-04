package com.airepublic.microprofile.module.http.websocket.server;

/**
 * This Tomcat specific interface is implemented by handlers that require direct access to Tomcat's
 * I/O layer rather than going through the Servlet API.
 */
public interface InternalHttpUpgradeHandler {

    void pause();


    /**
     * It is called when the client is disconnected.
     */
    void destroy();
}