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

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.websocket.server.HandshakeRequest;

/**
 * Represents the request that this session was opened under.
 */
public class WsHandshakeRequest implements HandshakeRequest {

    private final URI requestUri;
    private final Map<String, List<String>> parameterMap;
    private final String queryString;
    private final Principal userPrincipal;
    private final Map<String, List<String>> headers;


    public WsHandshakeRequest(final URI requestUri, final Map<String, List<String>> headers, final Principal userPrincipal, final Map<String, String> pathParams) {

        this.requestUri = requestUri;
        this.headers = Collections.unmodifiableMap(headers);
        this.userPrincipal = userPrincipal;
        queryString = requestUri.getQuery();

        // ParameterMap
        parameterMap = Collections.unmodifiableMap(parseQueryParams(requestUri.getQuery()));

    }


    private Map<String, List<String>> parseQueryParams(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }

        final Map<String, List<String>> params = new HashMap<>();

        if (query.startsWith("?")) {
            query = query.substring(1);
        }

        final StringTokenizer tokenizer = new StringTokenizer(query, "&");

        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            String key = null;
            String value = null;

            if (token.contains("=")) {
                final String[] entry = token.strip().split("=");
                key = entry[0];
                value = entry[1];
            } else {
                key = token.strip();
            }

            if (key != null) {
                List<String> values = params.get(key);

                if (values == null) {
                    values = new ArrayList<>();
                    params.put(key, values);
                }

                if (value != null) {
                    values.add(value);
                }
            }
        }

        return params;
    }


    @Override
    public URI getRequestURI() {
        return requestUri;
    }


    @Override
    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }


    @Override
    public String getQueryString() {
        return queryString;
    }


    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }


    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }


    @Override
    public boolean isUserInRole(final String role) {
        return false;
    }


    @Override
    public Object getHttpSession() {
        return null;
    }

}
