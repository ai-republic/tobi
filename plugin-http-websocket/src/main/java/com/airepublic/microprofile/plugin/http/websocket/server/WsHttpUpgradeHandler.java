/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.airepublic.microprofile.plugin.http.websocket.server;

import java.util.List;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import com.airepublic.microprofile.plugin.http.websocket.Transformation;
import com.airepublic.microprofile.plugin.http.websocket.WsSession;
import com.airepublic.microprofile.plugin.http.websocket.util.res.StringManager;

/**
 * Servlet 3.1 HTTP upgrade handler for WebSocket connections.
 */
public class WsHttpUpgradeHandler implements InternalHttpUpgradeHandler {

    private static final StringManager sm = StringManager.getManager(WsHttpUpgradeHandler.class);

    private final ClassLoader applicationClassLoader;

    private Endpoint ep;
    private ServerEndpointConfig serverEndpointConfig;
    private WsServerContainer webSocketContainer;
    private WsHandshakeRequest handshakeRequest;
    private List<Extension> negotiatedExtensions;
    private String subProtocol;
    private Transformation transformation;
    private Map<String, String> pathParameters;
    private boolean secure;

    private WsRemoteEndpointImplServer wsRemoteEndpointServer;
    private WsFrameServer wsFrame;
    private WsSession wsSession;


    public WsHttpUpgradeHandler() {
        applicationClassLoader = Thread.currentThread().getContextClassLoader();
    }


    public void preInit(final Endpoint ep, final ServerEndpointConfig serverEndpointConfig,
            final WsServerContainer wsc, final WsHandshakeRequest handshakeRequest,
            final List<Extension> negotiatedExtensionsPhase2, final String subProtocol,
            final Transformation transformation, final Map<String, String> pathParameters,
            final boolean secure) {
        this.ep = ep;
        this.serverEndpointConfig = serverEndpointConfig;
        webSocketContainer = wsc;
        this.handshakeRequest = handshakeRequest;
        negotiatedExtensions = negotiatedExtensionsPhase2;
        this.subProtocol = subProtocol;
        this.transformation = transformation;
        this.pathParameters = pathParameters;
        this.secure = secure;
    }


    @Override
    public void pause() {
        // NO-OP
    }


    @Override
    public void destroy() {
    }


    private void onError(final Throwable throwable) {
        // Need to call onError using the web application's class loader
        final Thread t = Thread.currentThread();
        final ClassLoader cl = t.getContextClassLoader();
        t.setContextClassLoader(applicationClassLoader);
        try {
            ep.onError(wsSession, throwable);
        } finally {
            t.setContextClassLoader(cl);
        }
    }


    private void close(final CloseReason cr) {
        /*
         * Any call to this method is a result of a problem reading from the client. At this point
         * that state of the connection is unknown. Attempt to send a close frame to the client and
         * then close the socket immediately. There is no point in waiting for a close frame from
         * the client because there is no guarantee that we can recover from whatever messed up
         * state the client put the connection into.
         */
        wsSession.onClose(cr);
    }


    public Endpoint getEp() {
        return ep;
    }


    public void setEp(final Endpoint ep) {
        this.ep = ep;
    }


    public ServerEndpointConfig getServerEndpointConfig() {
        return serverEndpointConfig;
    }


    public void setServerEndpointConfig(final ServerEndpointConfig serverEndpointConfig) {
        this.serverEndpointConfig = serverEndpointConfig;
    }


    public WsHandshakeRequest getHandshakeRequest() {
        return handshakeRequest;
    }


    public void setHandshakeRequest(final WsHandshakeRequest handshakeRequest) {
        this.handshakeRequest = handshakeRequest;
    }


    public List<Extension> getNegotiatedExtensions() {
        return negotiatedExtensions;
    }


    public void setNegotiatedExtensions(final List<Extension> negotiatedExtensions) {
        this.negotiatedExtensions = negotiatedExtensions;
    }


    public String getSubProtocol() {
        return subProtocol;
    }


    public void setSubProtocol(final String subProtocol) {
        this.subProtocol = subProtocol;
    }


    public Transformation getTransformation() {
        return transformation;
    }


    public void setTransformation(final Transformation transformation) {
        this.transformation = transformation;
    }


    public Map<String, String> getPathParameters() {
        return pathParameters;
    }


    public void setPathParameters(final Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }


    public boolean isSecure() {
        return secure;
    }


    public void setSecure(final boolean secure) {
        this.secure = secure;
    }
}
