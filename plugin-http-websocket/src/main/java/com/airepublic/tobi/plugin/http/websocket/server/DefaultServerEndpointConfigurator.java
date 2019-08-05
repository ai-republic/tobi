/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airepublic.tobi.plugin.http.websocket.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class DefaultServerEndpointConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(final Class<T> clazz) throws InstantiationException {
        try {
            if (CDI.current() != null) {
                return CDI.current().select(clazz).get();
            }
            return clazz.getConstructor().newInstance();
        } catch (final Throwable e) {
            throw new RuntimeException("Could not instantiate class " + clazz, e);
        }
    }


    @Override
    public String getNegotiatedSubprotocol(final List<String> supported, final List<String> requested) {

        for (final String request : requested) {
            if (supported.contains(request)) {
                return request;
            }
        }
        return "";
    }


    @Override
    public List<Extension> getNegotiatedExtensions(final List<Extension> installed, final List<Extension> requested) {
        final Set<String> installedNames = new HashSet<>();

        for (final Extension e : installed) {
            installedNames.add(e.getName());
        }

        final List<Extension> result = new ArrayList<>();

        for (final Extension request : requested) {
            if (installedNames.contains(request.getName())) {
                result.add(request);
            }
        }
        return result;
    }


    @Override
    public boolean checkOrigin(final String originHeaderValue) {
        return true;
    }


    @Override
    public void modifyHandshake(final ServerEndpointConfig sec, final HandshakeRequest request, final HandshakeResponse response) {
        // NO-OP
    }

}
