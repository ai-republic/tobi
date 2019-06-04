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
package com.airepublic.microprofile.module.http.websocket.server;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Registers an interest in any class that is annotated with {@link ServerEndpoint} so that Endpoint
 * can be published via the WebSocket server.
 */
public class WsSci {

    public static WsServerContainer onStartup(final Set<Class<?>> clazzes) throws IOException {

        final WsServerContainer sc = new WsServerContainer();

        if (clazzes == null || clazzes.size() == 0) {
            return sc;
        }

        // Group the discovered classes by type
        final Set<ServerApplicationConfig> serverApplicationConfigs = new HashSet<>();
        final Set<Class<? extends Endpoint>> scannedEndpointClazzes = new HashSet<>();
        final Set<Class<?>> scannedPojoEndpoints = new HashSet<>();

        try {
            // wsPackage is "javax.websocket."
            String wsPackage = ContainerProvider.class.getName();
            wsPackage = wsPackage.substring(0, wsPackage.lastIndexOf('.') + 1);

            for (final Class<?> clazz : clazzes) {
                final int modifiers = clazz.getModifiers();

                if (!Modifier.isPublic(modifiers) ||
                        Modifier.isAbstract(modifiers)) {
                    // Non-public or abstract - skip it.
                    continue;
                }

                // Protect against scanning the WebSocket API JARs
                if (clazz.getName().startsWith(wsPackage)) {
                    continue;
                }

                if (ServerApplicationConfig.class.isAssignableFrom(clazz)) {
                    serverApplicationConfigs.add((ServerApplicationConfig) clazz.getConstructor().newInstance());
                }

                if (Endpoint.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    final Class<? extends Endpoint> endpoint = (Class<? extends Endpoint>) clazz;
                    scannedEndpointClazzes.add(endpoint);
                }

                if (clazz.isAnnotationPresent(ServerEndpoint.class)) {
                    scannedPojoEndpoints.add(clazz);
                }
            }
        } catch (final ReflectiveOperationException e) {
            throw new IOException(e);
        }

        // Filter the results
        final Set<ServerEndpointConfig> filteredEndpointConfigs = new HashSet<>();
        final Set<Class<?>> filteredPojoEndpoints = new HashSet<>();

        if (serverApplicationConfigs.isEmpty()) {
            filteredPojoEndpoints.addAll(scannedPojoEndpoints);
        } else {
            for (final ServerApplicationConfig config : serverApplicationConfigs) {
                final Set<ServerEndpointConfig> configFilteredEndpoints = config.getEndpointConfigs(scannedEndpointClazzes);

                if (configFilteredEndpoints != null) {
                    filteredEndpointConfigs.addAll(configFilteredEndpoints);
                }

                final Set<Class<?>> configFilteredPojos = config.getAnnotatedEndpointClasses(scannedPojoEndpoints);

                if (configFilteredPojos != null) {
                    filteredPojoEndpoints.addAll(configFilteredPojos);
                }
            }
        }

        try {
            // Deploy endpoints
            for (final ServerEndpointConfig config : filteredEndpointConfigs) {
                sc.addEndpoint(config);
            }
            // Deploy POJOs
            for (final Class<?> clazz : filteredPojoEndpoints) {
                sc.addEndpoint(clazz);
            }
        } catch (final DeploymentException e) {
            throw new IOException(e);
        }

        return sc;
    }

}
