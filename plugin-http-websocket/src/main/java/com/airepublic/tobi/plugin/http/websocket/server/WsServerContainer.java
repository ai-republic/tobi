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
package com.airepublic.tobi.plugin.http.websocket.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import com.airepublic.http.common.pathmatcher.PathMapping;
import com.airepublic.http.common.pathmatcher.TemplatePathMatch;
import com.airepublic.tobi.plugin.http.websocket.SimpleInstanceManager;
import com.airepublic.tobi.plugin.http.websocket.WsSession;
import com.airepublic.tobi.plugin.http.websocket.WsWebSocketContainer;
import com.airepublic.tobi.plugin.http.websocket.pojo.PojoMethodMapping;
import com.airepublic.tobi.plugin.http.websocket.util.res.StringManager;

/**
 * Provides a per class loader (i.e. per web application) instance of a ServerContainer. Web
 * application wide defaults may be configured by setting the following servlet context
 * initialisation parameters to the desired values.
 * <ul>
 * <li>{@link Constants#BINARY_BUFFER_SIZE_SERVLET_CONTEXT_INIT_PARAM}</li>
 * <li>{@link Constants#TEXT_BUFFER_SIZE_SERVLET_CONTEXT_INIT_PARAM}</li>
 * </ul>
 */
public class WsServerContainer extends WsWebSocketContainer implements ServerContainer {

    private static final StringManager sm = StringManager.getManager(WsServerContainer.class);

    private static final CloseReason AUTHENTICATED_HTTP_SESSION_CLOSED = new CloseReason(CloseCodes.VIOLATED_POLICY, "This connection was established under an authenticated HTTP session that has ended.");

    private final WsWriteTimeout wsWriteTimeout = new WsWriteTimeout();

    private final Map<String, ServerEndpointConfig> configExactMatchMap = new ConcurrentHashMap<>();
    private final Map<Integer, SortedSet<TemplatePathMatch<ServerEndpointConfig>>> configTemplateMatchMap = new ConcurrentHashMap<>();
    private volatile boolean enforceNoAddAfterHandshake = com.airepublic.tobi.plugin.http.websocket.Constants.STRICT_SPEC_COMPLIANCE;
    private volatile boolean addAllowed = true;
    private final Map<String, Set<WsSession>> authenticatedSessions = new ConcurrentHashMap<>();
    private volatile boolean endpointsRegistered = false;
    private volatile boolean deploymentFailed = false;
    private final PathMapping<ServerEndpointConfig> mapping = new PathMapping<>();


    WsServerContainer() {
        setInstanceManager(new SimpleInstanceManager());

        // Configure servlet context wide defaults
        setDefaultMaxBinaryMessageBufferSize(8 * 1024);
        setDefaultMaxTextMessageBufferSize(8 * 1024);
        setEnforceNoAddAfterHandshake(false);

        // final FilterRegistration.Dynamic fr = servletContext.addFilter(
        // "Tomcat WebSocket (JSR356) Filter", new WsFilter());
        // fr.setAsyncSupported(true);
        //
        // final EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST,
        // DispatcherType.FORWARD);
        //
        // fr.addMappingForUrlPatterns(types, true, "/*");
    }


    /**
     * Published the provided endpoint implementation at the specified path with the specified
     * configuration. {@link #WsServerContainer(ServletContext)} must be called before calling this
     * method.
     *
     * @param sec The configuration to use when creating endpoint instances
     * @throws DeploymentException if the endpoint cannot be published as requested
     */
    @Override
    public void addEndpoint(final ServerEndpointConfig sec) throws DeploymentException {

        if (enforceNoAddAfterHandshake && !addAllowed) {
            throw new DeploymentException(sm.getString("serverContainer.addNotAllowed"));
        }

        if (deploymentFailed) {
            throw new DeploymentException(sm.getString("serverContainer.failedDeployment", "", ""));
        }

        try {
            final String path = sec.getPath();

            // Add method mapping to user properties
            final PojoMethodMapping methodMapping = new PojoMethodMapping(sec.getEndpointClass(), sec.getDecoders(), path);

            if (methodMapping.getOnClose() != null || methodMapping.getOnOpen() != null || methodMapping.getOnError() != null || methodMapping.hasMessageHandlers()) {
                sec.getUserProperties().put(com.airepublic.tobi.plugin.http.websocket.pojo.Constants.POJO_METHOD_MAPPING_KEY, methodMapping);
            }

            mapping.add(path, sec);

            endpointsRegistered = true;
        } catch (final DeploymentException de) {
            failDeployment();
            throw de;
        }
    }


    /**
     * Provides the equivalent of {@link #addEndpoint(ServerEndpointConfig)} for publishing plain
     * old java objects (POJOs) that have been annotated as WebSocket endpoints.
     *
     * @param pojo The annotated POJO
     */
    @Override
    public void addEndpoint(final Class<?> pojo) throws DeploymentException {

        if (deploymentFailed) {
            throw new DeploymentException(sm.getString("serverContainer.failedDeployment", "", ""));
        }

        ServerEndpointConfig sec;

        try {
            final ServerEndpoint annotation = pojo.getAnnotation(ServerEndpoint.class);

            if (annotation == null) {
                throw new DeploymentException(sm.getString("serverContainer.missingAnnotation", pojo.getName()));
            }

            final String path = annotation.value();

            // Validate encoders
            validateEncoders(annotation.encoders());

            // ServerEndpointConfig
            final Class<? extends Configurator> configuratorClazz = annotation.configurator();
            Configurator configurator = null;

            if (!configuratorClazz.equals(Configurator.class)) {
                try {
                    configurator = annotation.configurator().getConstructor().newInstance();
                } catch (final ReflectiveOperationException e) {
                    throw new DeploymentException(sm.getString("serverContainer.configuratorFail", annotation.configurator().getName(), pojo.getClass().getName()), e);
                }
            }

            sec = ServerEndpointConfig.Builder.create(pojo, path).decoders(Arrays.asList(annotation.decoders())).encoders(Arrays.asList(annotation.encoders())).subprotocols(Arrays.asList(annotation.subprotocols())).configurator(configurator).build();
        } catch (final DeploymentException de) {
            failDeployment();
            throw de;
        }

        addEndpoint(sec);
    }


    public PathMapping<ServerEndpointConfig> getMapping() {
        return mapping;
    }


    void failDeployment() {
        deploymentFailed = true;

        // Clear all existing deployments
        endpointsRegistered = false;
        configExactMatchMap.clear();
        configTemplateMatchMap.clear();
    }


    boolean areEndpointsRegistered() {
        return endpointsRegistered;
    }


    public boolean isEnforceNoAddAfterHandshake() {
        return enforceNoAddAfterHandshake;
    }


    public void setEnforceNoAddAfterHandshake(
            final boolean enforceNoAddAfterHandshake) {
        this.enforceNoAddAfterHandshake = enforceNoAddAfterHandshake;
    }


    protected WsWriteTimeout getTimeout() {
        return wsWriteTimeout;
    }


    /**
     * {@inheritDoc}
     *
     * Overridden to make it visible to other classes in this package.
     */
    @Override
    public void registerSession(final Object key, final WsSession wsSession) {
        super.registerSession(key, wsSession);
        if (wsSession.isOpen() &&
                wsSession.getUserPrincipal() != null &&
                wsSession.getHttpSessionId() != null) {
            registerAuthenticatedSession(wsSession,
                    wsSession.getHttpSessionId());
        }
    }


    /**
     * {@inheritDoc}
     *
     * Overridden to make it visible to other classes in this package.
     */
    @Override
    protected void unregisterSession(final Object key, final WsSession wsSession) {
        if (wsSession.getUserPrincipal() != null &&
                wsSession.getHttpSessionId() != null) {
            unregisterAuthenticatedSession(wsSession,
                    wsSession.getHttpSessionId());
        }
        super.unregisterSession(key, wsSession);
    }


    private void registerAuthenticatedSession(final WsSession wsSession,
            final String httpSessionId) {
        Set<WsSession> wsSessions = authenticatedSessions.get(httpSessionId);
        if (wsSessions == null) {
            wsSessions = Collections.newSetFromMap(
                    new ConcurrentHashMap<WsSession, Boolean>());
            authenticatedSessions.putIfAbsent(httpSessionId, wsSessions);
            wsSessions = authenticatedSessions.get(httpSessionId);
        }
        wsSessions.add(wsSession);
    }


    private void unregisterAuthenticatedSession(final WsSession wsSession,
            final String httpSessionId) {
        final Set<WsSession> wsSessions = authenticatedSessions.get(httpSessionId);
        // wsSessions will be null if the HTTP session has ended
        if (wsSessions != null) {
            wsSessions.remove(wsSession);
        }
    }


    public void closeAuthenticatedSession(final String httpSessionId) {
        final Set<WsSession> wsSessions = authenticatedSessions.remove(httpSessionId);

        if (wsSessions != null && !wsSessions.isEmpty()) {
            for (final WsSession wsSession : wsSessions) {
                try {
                    wsSession.close(AUTHENTICATED_HTTP_SESSION_CLOSED);
                } catch (final IOException e) {
                    // Any IOExceptions during close will have been caught and the
                    // onError method called.
                }
            }
        }
    }


    private static void validateEncoders(final Class<? extends Encoder>[] encoders)
            throws DeploymentException {

        for (final Class<? extends Encoder> encoder : encoders) {
            // Need to instantiate decoder to ensure it is valid and that
            // deployment can be failed if it is not
            @SuppressWarnings("unused")
            final Encoder instance;
            try {
                encoder.getConstructor().newInstance();
            } catch (final ReflectiveOperationException e) {
                throw new DeploymentException(sm.getString(
                        "serverContainer.encoderFail", encoder.getName()), e);
            }
        }
    }
}
