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

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.ServerEndpointConfig;

import com.airepublic.tobi.plugin.http.websocket.Constants;
import com.airepublic.tobi.plugin.http.websocket.Transformation;
import com.airepublic.tobi.plugin.http.websocket.TransformationFactory;
import com.airepublic.tobi.plugin.http.websocket.Util;
import com.airepublic.tobi.plugin.http.websocket.WsHandshakeResponse;
import com.airepublic.tobi.plugin.http.websocket.pojo.PojoEndpointServer;
import com.airepublic.tobi.plugin.http.websocket.util.codec.binary.Base64;
import com.airepublic.tobi.plugin.http.websocket.util.res.StringManager;
import com.airepublic.tobi.plugin.http.websocket.util.security.ConcurrentMessageDigest;

public class UpgradeUtil {

    private static final StringManager sm = StringManager.getManager(UpgradeUtil.class.getPackage().getName());
    private static final byte[] WS_ACCEPT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(
            StandardCharsets.ISO_8859_1);


    private UpgradeUtil() {
        // Utility class. Hide default constructor.
    }


    /**
     * Checks to see if this is an HTTP request that includes a valid upgrade request to web socket.
     * <p>
     * Note: RFC 2616 does not limit HTTP upgrade to GET requests but the Java WebSocket spec 1.0,
     * section 8.2 implies such a limitation and RFC 6455 section 4.1 requires that a WebSocket
     * Upgrade uses GET.
     * 
     * @param request The request to check if it is an HTTP upgrade request for a WebSocket
     *        connection
     * @param response The response associated with the request
     * @return <code>true</code> if the request includes a HTTP Upgrade request for the WebSocket
     *         protocol, otherwise <code>false</code>
     */
    public static boolean isWebSocketUpgradeRequest(final String method, final Map<String, List<String>> reqHeaders) {

        return headerContainsToken(reqHeaders, Constants.UPGRADE_HEADER_NAME, Constants.UPGRADE_HEADER_VALUE) && "GET".equals(method);
    }


    public static ByteBuffer doUpgrade(final WsServerContainer sc, final URI requestUri, final Map<String, List<String>> reqHeaders, final Principal userPrincipal, final ServerEndpointConfig sec, final Map<String, String> pathParams, final WsHttpUpgradeHandler handler) throws IOException {

        // Validate the rest of the headers and reject the request if that
        // validation fails
        String key;
        String subProtocol = null;

        if (!headerContainsToken(reqHeaders, Constants.CONNECTION_HEADER_NAME, Constants.CONNECTION_HEADER_VALUE)) {
            return createResponse(400, "Bad Request", null);
        }
        if (!headerContainsToken(reqHeaders, Constants.WS_VERSION_HEADER_NAME, Constants.WS_VERSION_HEADER_VALUE)) {
            final Map<String, List<String>> headers = new HashMap<>();
            headers.put(Constants.WS_VERSION_HEADER_NAME, Arrays.asList(Constants.WS_VERSION_HEADER_VALUE));
            return createResponse(426, "Upgrade Required", headers);
        }

        List<String> values = reqHeaders.get(Constants.WS_KEY_HEADER_NAME);

        if (values == null || values.isEmpty()) {
            return createResponse(400, "Bad Request", null);
        }

        key = values.get(0);

        // Origin check
        values = reqHeaders.get(Constants.ORIGIN_HEADER_NAME);

        final String origin = values == null || values.isEmpty() ? null : values.get(0);

        if (!sec.getConfigurator().checkOrigin(origin)) {
            return createResponse(401, "FORBIDDEN", null);
        }

        // Sub-protocols
        final List<String> subProtocols = getTokensFromHeader(reqHeaders, Constants.WS_PROTOCOL_HEADER_NAME);
        subProtocol = sec.getConfigurator().getNegotiatedSubprotocol(sec.getSubprotocols(), subProtocols);

        // Extensions
        // Should normally only be one header but handle the case of multiple
        // headers
        final List<Extension> extensionsRequested = new ArrayList<>();
        final List<String> extHeaders = reqHeaders.get(Constants.WS_EXTENSIONS_HEADER_NAME);

        if (extHeaders != null) {
            for (final String extHeader : extHeaders) {
                Util.parseExtensionHeader(extensionsRequested, extHeader);
            }
        }

        // Negotiation phase 1. By default this simply filters out the
        // extensions that the server does not support but applications could
        // use a custom configurator to do more than this.
        List<Extension> installedExtensions = null;

        if (sec.getExtensions().size() == 0) {
            installedExtensions = Constants.INSTALLED_EXTENSIONS;
        } else {
            installedExtensions = new ArrayList<>();
            installedExtensions.addAll(sec.getExtensions());
            installedExtensions.addAll(Constants.INSTALLED_EXTENSIONS);
        }

        final List<Extension> negotiatedExtensionsPhase1 = sec.getConfigurator().getNegotiatedExtensions(installedExtensions, extensionsRequested);

        // Negotiation phase 2. Create the Transformations that will be applied
        // to this connection. Note than an extension may be dropped at this
        // point if the client has requested a configuration that the server is
        // unable to support.
        final List<Transformation> transformations = createTransformations(negotiatedExtensionsPhase1);

        List<Extension> negotiatedExtensionsPhase2;

        if (transformations.isEmpty()) {
            negotiatedExtensionsPhase2 = Collections.emptyList();
        } else {
            negotiatedExtensionsPhase2 = new ArrayList<>(transformations.size());
            for (final Transformation t : transformations) {
                negotiatedExtensionsPhase2.add(t.getExtensionResponse());
            }
        }

        // Build the transformation pipeline
        Transformation transformation = null;
        final StringBuilder responseHeaderExtensions = new StringBuilder();
        boolean first = true;

        for (final Transformation t : transformations) {
            if (first) {
                first = false;
            } else {
                responseHeaderExtensions.append(',');
            }
            append(responseHeaderExtensions, t.getExtensionResponse());
            if (transformation == null) {
                transformation = t;
            } else {
                transformation.setNext(t);
            }
        }

        // Now we have the full pipeline, validate the use of the RSV bits.
        if (transformation != null && !transformation.validateRsvBits(0)) {
            throw new IOException(sm.getString("upgradeUtil.incompatibleRsv"));
        }

        // If we got this far, all is good. Accept the connection.
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.UPGRADE_HEADER_NAME, Arrays.asList(Constants.UPGRADE_HEADER_VALUE));
        headers.put(Constants.CONNECTION_HEADER_NAME, Arrays.asList(Constants.CONNECTION_HEADER_VALUE));
        headers.put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, Arrays.asList(getWebSocketAccept(key)));

        if (subProtocol != null && subProtocol.length() > 0) {
            // RFC6455 4.2.2 explicitly states "" is not valid here
            headers.put(Constants.WS_PROTOCOL_HEADER_NAME, Arrays.asList(subProtocol));
        }
        if (!transformations.isEmpty()) {
            headers.put(Constants.WS_EXTENSIONS_HEADER_NAME, Arrays.asList(responseHeaderExtensions.toString()));
        }

        final WsHandshakeRequest wsRequest = new WsHandshakeRequest(requestUri, reqHeaders, userPrincipal, pathParams);
        final WsHandshakeResponse wsResponse = new WsHandshakeResponse();
        final WsPerSessionServerEndpointConfig perSessionServerEndpointConfig = new WsPerSessionServerEndpointConfig(sec);
        sec.getConfigurator().modifyHandshake(perSessionServerEndpointConfig, wsRequest, wsResponse);

        // Add any additional headers
        for (final Entry<String, List<String>> entry : wsResponse.getHeaders().entrySet()) {
            for (final String headerValue : entry.getValue()) {
                headers.put(entry.getKey(), Arrays.asList(headerValue));
            }
        }

        Endpoint ep;
        try {
            final Class<?> clazz = sec.getEndpointClass();
            if (Endpoint.class.isAssignableFrom(clazz)) {
                ep = (Endpoint) sec.getConfigurator().getEndpointInstance(clazz);
            } else {
                ep = new PojoEndpointServer();
                // Need to make path params available to POJO
                perSessionServerEndpointConfig.getUserProperties().put(com.airepublic.tobi.plugin.http.websocket.pojo.Constants.POJO_PATH_PARAM_KEY, pathParams);
            }
        } catch (final InstantiationException e) {
            throw new IOException(e);
        }

        handler.preInit(ep, perSessionServerEndpointConfig, sc, wsRequest, negotiatedExtensionsPhase2, subProtocol, transformation, pathParams, requestUri.getScheme().equalsIgnoreCase("https") || requestUri.getScheme().equalsIgnoreCase("wss"));

        return createResponse(101, "Switching Protocols", headers);
    }


    private static List<Transformation> createTransformations(final List<Extension> negotiatedExtensions) {

        final TransformationFactory factory = TransformationFactory.getInstance();

        final LinkedHashMap<String, List<List<Extension.Parameter>>> extensionPreferences = new LinkedHashMap<>();

        // Result will likely be smaller than this
        final List<Transformation> result = new ArrayList<>(negotiatedExtensions.size());

        for (final Extension extension : negotiatedExtensions) {
            List<List<Extension.Parameter>> preferences = extensionPreferences.get(extension.getName());

            if (preferences == null) {
                preferences = new ArrayList<>();
                extensionPreferences.put(extension.getName(), preferences);
            }

            preferences.add(extension.getParameters());
        }

        for (final Map.Entry<String, List<List<Extension.Parameter>>> entry : extensionPreferences.entrySet()) {
            final Transformation transformation = factory.create(entry.getKey(), entry.getValue(), true);
            if (transformation != null) {
                result.add(transformation);
            }
        }
        return result;
    }


    private static void append(final StringBuilder sb, final Extension extension) {
        if (extension == null || extension.getName() == null || extension.getName().length() == 0) {
            return;
        }

        sb.append(extension.getName());

        for (final Extension.Parameter p : extension.getParameters()) {
            sb.append(';');
            sb.append(p.getName());
            if (p.getValue() != null) {
                sb.append('=');
                sb.append(p.getValue());
            }
        }
    }


    /*
     * This only works for tokens. Quoted strings need more sophisticated parsing.
     */
    private static boolean headerContainsToken(final Map<String, List<String>> headers, final String headerName, final String target) {
        final List<String> values = headers.get(headerName);

        if (values != null) {
            for (final String value : values) {
                final String[] tokens = value.split(",");

                for (final String token : tokens) {
                    if (target.equalsIgnoreCase(token.strip())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /*
     * This only works for tokens. Quoted strings need more sophisticated parsing.
     */
    private static List<String> getTokensFromHeader(final Map<String, List<String>> headers, final String headerName) {
        final List<String> result = new ArrayList<>();
        final List<String> values = headers.get(headerName);

        if (values != null) {
            for (final String value : values) {
                final String[] tokens = value.split(",");

                for (final String token : tokens) {
                    result.add(token.strip());
                }
            }
        }

        return result;
    }


    private static String getWebSocketAccept(final String key) {
        final byte[] digest = ConcurrentMessageDigest.digestSHA1(key.getBytes(StandardCharsets.ISO_8859_1), WS_ACCEPT);
        return Base64.encodeBase64String(digest);
    }


    static ByteBuffer createResponse(final int statusCode, final String statusDesc, final Map<String, List<String>> headers) {
        final StringBuffer str = new StringBuffer();
        str.append("HTTP/1.1 " + statusCode + " " + statusDesc + "\r\n");

        if (headers != null) {
            final StringBuffer headerBuf = headers.entrySet().stream().map(entry -> {
                final StringBuffer buf = new StringBuffer();
                entry.getValue().stream().forEach(value -> buf.append(entry.getKey() + ": " + value + "\r\n"));
                return buf;
            }).collect(StringBuffer::new, StringBuffer::append, StringBuffer::append);

            str.append(headerBuf);
            str.append("\r\n");
        }

        return ByteBuffer.wrap(str.toString().getBytes());
    }

}
