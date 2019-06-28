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
package com.airepublic.microprofile.plugin.http.websocket;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.airepublic.microprofile.feature.logging.java.SerializableLogger;
import com.airepublic.microprofile.plugin.http.websocket.pojo.PojoEndpointClient;
import com.airepublic.microprofile.plugin.http.websocket.util.buf.StringUtils;
import com.airepublic.microprofile.plugin.http.websocket.util.codec.binary.Base64;
import com.airepublic.microprofile.plugin.http.websocket.util.collections.CaseInsensitiveKeyMap;
import com.airepublic.microprofile.plugin.http.websocket.util.res.StringManager;
import com.airepublic.microprofile.plugin.http.websocket.util.security.KeyStoreUtil;

public class WsWebSocketContainer implements WebSocketContainer, BackgroundProcess {

    private static final StringManager sm = StringManager.getManager(WsWebSocketContainer.class);
    private static final Random RANDOM = new Random();
    private static final byte[] CRLF = new byte[] { 13, 10 };

    private static final byte[] GET_BYTES = "GET ".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] ROOT_URI_BYTES = "/".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] HTTP_VERSION_BYTES = " HTTP/1.1\r\n".getBytes(StandardCharsets.ISO_8859_1);

    private volatile AsynchronousChannelGroup asynchronousChannelGroup = null;
    private final Object asynchronousChannelGroupLock = new Object();

    private final Logger log = new SerializableLogger(WsWebSocketContainer.class.getName()); // must not
                                                                                       // be
    // static
    // Server side uses the endpoint path as the key
    // Client side uses the client endpoint instance
    private final Map<Object, Set<WsSession>> endpointSessionMap = new HashMap<>();
    private final Map<WsSession, WsSession> sessions = new ConcurrentHashMap<>();
    private final Object endPointSessionMapLock = new Object();

    private long defaultAsyncTimeout = -1;
    private int maxBinaryMessageBufferSize = Constants.DEFAULT_BUFFER_SIZE;
    private int maxTextMessageBufferSize = Constants.DEFAULT_BUFFER_SIZE;
    private volatile long defaultMaxSessionIdleTimeout = 0;
    private int backgroundProcessCount = 0;
    private int processPeriod = Constants.DEFAULT_PROCESS_PERIOD;

    private InstanceManager instanceManager;


    InstanceManager getInstanceManager() {
        return instanceManager;
    }


    protected void setInstanceManager(final InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }


    @Override
    public Session connectToServer(final Object pojo, final URI path)
            throws DeploymentException {

        final ClientEndpoint annotation = pojo.getClass().getAnnotation(ClientEndpoint.class);
        if (annotation == null) {
            throw new DeploymentException(
                    sm.getString("wsWebSocketContainer.missingAnnotation",
                            pojo.getClass().getName()));
        }

        final Endpoint ep = new PojoEndpointClient(pojo, Arrays.asList(annotation.decoders()));

        final Class<? extends ClientEndpointConfig.Configurator> configuratorClazz = annotation.configurator();

        ClientEndpointConfig.Configurator configurator = null;
        if (!ClientEndpointConfig.Configurator.class.equals(
                configuratorClazz)) {
            try {
                configurator = configuratorClazz.getConstructor().newInstance();
            } catch (final ReflectiveOperationException e) {
                throw new DeploymentException(sm.getString(
                        "wsWebSocketContainer.defaultConfiguratorFail"), e);
            }
        }

        final ClientEndpointConfig.Builder builder = ClientEndpointConfig.Builder.create();
        // Avoid NPE when using RI API JAR - see BZ 56343
        if (configurator != null) {
            builder.configurator(configurator);
        }
        final ClientEndpointConfig config = builder.decoders(Arrays.asList(annotation.decoders())).encoders(Arrays.asList(annotation.encoders())).preferredSubprotocols(Arrays.asList(annotation.subprotocols())).build();
        return connectToServer(ep, config, path);
    }


    @Override
    public Session connectToServer(final Class<?> annotatedEndpointClass, final URI path)
            throws DeploymentException {

        Object pojo;
        try {
            pojo = annotatedEndpointClass.getConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.endpointCreateFail",
                    annotatedEndpointClass.getName()), e);
        }

        return connectToServer(pojo, path);
    }


    @Override
    public Session connectToServer(final Class<? extends Endpoint> clazz,
            final ClientEndpointConfig clientEndpointConfiguration, final URI path)
            throws DeploymentException {

        Endpoint endpoint;
        try {
            endpoint = clazz.getConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.endpointCreateFail", clazz.getName()),
                    e);
        }

        return connectToServer(endpoint, clientEndpointConfiguration, path);
    }


    @Override
    public Session connectToServer(final Endpoint endpoint,
            final ClientEndpointConfig clientEndpointConfiguration, final URI path)
            throws DeploymentException {
        return connectToServerRecursive(endpoint, clientEndpointConfiguration, path, new HashSet<>());
    }


    private Session connectToServerRecursive(final Endpoint endpoint,
            final ClientEndpointConfig clientEndpointConfiguration, final URI path,
            final Set<URI> redirectSet)
            throws DeploymentException {

        boolean secure = false;
        ByteBuffer proxyConnect = null;
        URI proxyPath;

        // Validate scheme (and build proxyPath)
        final String scheme = path.getScheme();
        if ("ws".equalsIgnoreCase(scheme)) {
            proxyPath = URI.create("http" + path.toString().substring(2));
        } else if ("wss".equalsIgnoreCase(scheme)) {
            proxyPath = URI.create("https" + path.toString().substring(3));
            secure = true;
        } else {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.pathWrongScheme", scheme));
        }

        // Validate host
        final String host = path.getHost();
        if (host == null) {
            throw new DeploymentException(
                    sm.getString("wsWebSocketContainer.pathNoHost"));
        }
        int port = path.getPort();

        SocketAddress sa = null;

        // Check to see if a proxy is configured. Javadoc indicates return value
        // will never be null
        final List<Proxy> proxies = ProxySelector.getDefault().select(proxyPath);
        Proxy selectedProxy = null;
        for (final Proxy proxy : proxies) {
            if (proxy.type().equals(Proxy.Type.HTTP)) {
                sa = proxy.address();
                if (sa instanceof InetSocketAddress) {
                    final InetSocketAddress inet = (InetSocketAddress) sa;
                    if (inet.isUnresolved()) {
                        sa = new InetSocketAddress(inet.getHostName(), inet.getPort());
                    }
                }
                selectedProxy = proxy;
                break;
            }
        }

        // If the port is not explicitly specified, compute it based on the
        // scheme
        if (port == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else {
                // Must be wss due to scheme validation above
                port = 443;
            }
        }

        // If sa is null, no proxy is configured so need to create sa
        if (sa == null) {
            sa = new InetSocketAddress(host, port);
        } else {
            proxyConnect = createProxyRequest(host, port);
        }

        // Create the initial HTTP request to open the WebSocket connection
        final Map<String, List<String>> reqHeaders = createRequestHeaders(host, port,
                clientEndpointConfiguration);
        clientEndpointConfiguration.getConfigurator().beforeRequest(reqHeaders);
        if (Constants.DEFAULT_ORIGIN_HEADER_VALUE != null
                && !reqHeaders.containsKey(Constants.ORIGIN_HEADER_NAME)) {
            final List<String> originValues = new ArrayList<>(1);
            originValues.add(Constants.DEFAULT_ORIGIN_HEADER_VALUE);
            reqHeaders.put(Constants.ORIGIN_HEADER_NAME, originValues);
        }
        final ByteBuffer request = createRequest(path, reqHeaders);

        AsynchronousSocketChannel socketChannel;
        try {
            socketChannel = AsynchronousSocketChannel.open(getAsynchronousChannelGroup());
        } catch (final IOException ioe) {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.asynchronousSocketChannelFail"), ioe);
        }

        final Map<String, Object> userProperties = clientEndpointConfiguration.getUserProperties();

        // Get the connection timeout
        long timeout = Constants.IO_TIMEOUT_MS_DEFAULT;
        final String timeoutValue = (String) userProperties.get(Constants.IO_TIMEOUT_MS_PROPERTY);
        if (timeoutValue != null) {
            timeout = Long.valueOf(timeoutValue).intValue();
        }

        // Set-up
        // Same size as the WsFrame input buffer
        final ByteBuffer response = ByteBuffer.allocate(getDefaultMaxBinaryMessageBufferSize());
        String subProtocol;
        boolean success = false;
        final List<Extension> extensionsAgreed = new ArrayList<>();
        Transformation transformation = null;

        // Open the connection
        final Future<Void> fConnect = socketChannel.connect(sa);
        AsyncChannelWrapper channel = null;

        if (proxyConnect != null) {
            try {
                fConnect.get(timeout, TimeUnit.MILLISECONDS);
                // Proxy CONNECT is clear text
                channel = new AsyncChannelWrapperNonSecure(socketChannel);
                writeRequest(channel, proxyConnect, timeout);
                final HttpResponse httpResponse = processResponse(response, channel, timeout);
                if (httpResponse.getStatus() != 200) {
                    throw new DeploymentException(sm.getString(
                            "wsWebSocketContainer.proxyConnectFail", selectedProxy,
                            Integer.toString(httpResponse.getStatus())));
                }
            } catch (TimeoutException | InterruptedException | ExecutionException | EOFException e) {
                if (channel != null) {
                    channel.close();
                }
                throw new DeploymentException(
                        sm.getString("wsWebSocketContainer.httpRequestFailed"), e);
            }
        }

        if (secure) {
            // Regardless of whether a non-secure wrapper was created for a
            // proxy CONNECT, need to use TLS from this point on so wrap the
            // original AsynchronousSocketChannel
            final SSLEngine sslEngine = createSSLEngine(userProperties, host, port);
            channel = new AsyncChannelWrapperSecure(socketChannel, sslEngine);
        } else if (channel == null) {
            // Only need to wrap as this point if it wasn't wrapped to process a
            // proxy CONNECT
            channel = new AsyncChannelWrapperNonSecure(socketChannel);
        }

        try {
            fConnect.get(timeout, TimeUnit.MILLISECONDS);

            final Future<Void> fHandshake = channel.handshake();
            fHandshake.get(timeout, TimeUnit.MILLISECONDS);

            writeRequest(channel, request, timeout);

            final HttpResponse httpResponse = processResponse(response, channel, timeout);

            // Check maximum permitted redirects
            int maxRedirects = Constants.MAX_REDIRECTIONS_DEFAULT;
            final String maxRedirectsValue = (String) userProperties.get(Constants.MAX_REDIRECTIONS_PROPERTY);
            if (maxRedirectsValue != null) {
                maxRedirects = Integer.parseInt(maxRedirectsValue);
            }

            if (httpResponse.status != 101) {
                if (isRedirectStatus(httpResponse.status)) {
                    final List<String> locationHeader = httpResponse.getHandshakeResponse().getHeaders().get(
                            Constants.LOCATION_HEADER_NAME);

                    if (locationHeader == null || locationHeader.isEmpty() ||
                            locationHeader.get(0) == null || locationHeader.get(0).isEmpty()) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.missingLocationHeader",
                                Integer.toString(httpResponse.status)));
                    }

                    URI redirectLocation = URI.create(locationHeader.get(0)).normalize();

                    if (!redirectLocation.isAbsolute()) {
                        redirectLocation = path.resolve(redirectLocation);
                    }

                    final String redirectScheme = redirectLocation.getScheme().toLowerCase();

                    if (redirectScheme.startsWith("http")) {
                        redirectLocation = new URI(redirectScheme.replace("http", "ws"),
                                redirectLocation.getUserInfo(), redirectLocation.getHost(),
                                redirectLocation.getPort(), redirectLocation.getPath(),
                                redirectLocation.getQuery(), redirectLocation.getFragment());
                    }

                    if (!redirectSet.add(redirectLocation) || redirectSet.size() > maxRedirects) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.redirectThreshold", redirectLocation,
                                Integer.toString(redirectSet.size()),
                                Integer.toString(maxRedirects)));
                    }

                    return connectToServerRecursive(endpoint, clientEndpointConfiguration, redirectLocation, redirectSet);

                }

                else if (httpResponse.status == 401) {

                    if (userProperties.get(Constants.AUTHORIZATION_HEADER_NAME) != null) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.failedAuthentication",
                                Integer.valueOf(httpResponse.status)));
                    }

                    final List<String> wwwAuthenticateHeaders = httpResponse.getHandshakeResponse()
                            .getHeaders().get(Constants.WWW_AUTHENTICATE_HEADER_NAME);

                    if (wwwAuthenticateHeaders == null || wwwAuthenticateHeaders.isEmpty() ||
                            wwwAuthenticateHeaders.get(0) == null || wwwAuthenticateHeaders.get(0).isEmpty()) {
                        throw new DeploymentException(sm.getString(
                                "wsWebSocketContainer.missingWWWAuthenticateHeader",
                                Integer.toString(httpResponse.status)));
                    }

                    final String authScheme = wwwAuthenticateHeaders.get(0).split("\\s+", 2)[0];
                    final String requestUri = new String(request.array(), StandardCharsets.ISO_8859_1)
                            .split("\\s", 3)[1];

                    final Authenticator auth = AuthenticatorFactory.getAuthenticator(authScheme);

                    if (auth == null) {
                        throw new DeploymentException(
                                sm.getString("wsWebSocketContainer.unsupportedAuthScheme",
                                        Integer.valueOf(httpResponse.status), authScheme));
                    }

                    userProperties.put(Constants.AUTHORIZATION_HEADER_NAME, auth.getAuthorization(
                            requestUri, wwwAuthenticateHeaders.get(0), userProperties));

                    return connectToServerRecursive(endpoint, clientEndpointConfiguration, path, redirectSet);

                }

                else {
                    throw new DeploymentException(sm.getString("wsWebSocketContainer.invalidStatus",
                            Integer.toString(httpResponse.status)));
                }
            }
            final HandshakeResponse handshakeResponse = httpResponse.getHandshakeResponse();
            clientEndpointConfiguration.getConfigurator().afterResponse(handshakeResponse);

            // Sub-protocol
            final List<String> protocolHeaders = handshakeResponse.getHeaders().get(
                    Constants.WS_PROTOCOL_HEADER_NAME);
            if (protocolHeaders == null || protocolHeaders.size() == 0) {
                subProtocol = null;
            } else if (protocolHeaders.size() == 1) {
                subProtocol = protocolHeaders.get(0);
            } else {
                throw new DeploymentException(
                        sm.getString("wsWebSocketContainer.invalidSubProtocol"));
            }

            // Extensions
            // Should normally only be one header but handle the case of
            // multiple headers
            final List<String> extHeaders = handshakeResponse.getHeaders().get(
                    Constants.WS_EXTENSIONS_HEADER_NAME);
            if (extHeaders != null) {
                for (final String extHeader : extHeaders) {
                    Util.parseExtensionHeader(extensionsAgreed, extHeader);
                }
            }

            // Build the transformations
            final TransformationFactory factory = TransformationFactory.getInstance();
            for (final Extension extension : extensionsAgreed) {
                final List<List<Extension.Parameter>> wrapper = new ArrayList<>(1);
                wrapper.add(extension.getParameters());
                final Transformation t = factory.create(extension.getName(), wrapper, false);
                if (t == null) {
                    throw new DeploymentException(sm.getString(
                            "wsWebSocketContainer.invalidExtensionParameters"));
                }
                if (transformation == null) {
                    transformation = t;
                } else {
                    transformation.setNext(t);
                }
            }

            success = true;
        } catch (ExecutionException | InterruptedException | SSLException | EOFException | TimeoutException | URISyntaxException | AuthenticationException e) {
            throw new DeploymentException(
                    sm.getString("wsWebSocketContainer.httpRequestFailed"), e);
        } finally {
            if (!success) {
                channel.close();
            }
        }

        // Switch to WebSocket
        final WsRemoteEndpointImplClient wsRemoteEndpointClient = new WsRemoteEndpointImplClient(channel);

        final WsSession wsSession = new WsSession(endpoint, wsRemoteEndpointClient,
                this, null, null, null, null, null, extensionsAgreed,
                subProtocol, Collections.<String, String> emptyMap(), secure,
                clientEndpointConfiguration);

        final WsFrameClient wsFrameClient = new WsFrameClient(response, channel,
                wsSession, transformation);
        // WsFrame adds the necessary final transformations. Copy the
        // completed transformation chain to the remote end point.
        wsRemoteEndpointClient.setTransformation(wsFrameClient.getTransformation());

        endpoint.onOpen(wsSession, clientEndpointConfiguration);
        registerSession(endpoint, wsSession);

        /*
         * It is possible that the server sent one or more messages as soon as the WebSocket
         * connection was established. Depending on the exact timing of when those messages were
         * sent they could be sat in the input buffer waiting to be read and will not trigger a
         * "data available to read" event. Therefore, it is necessary to process the input buffer
         * here. Note that this happens on the current thread which means that this thread will be
         * used for any onMessage notifications. This is a special case. Subsequent
         * "data available to read" events will be handled by threads from the AsyncChannelGroup's
         * executor.
         */
        wsFrameClient.startInputProcessing();

        return wsSession;
    }


    private static void writeRequest(final AsyncChannelWrapper channel, final ByteBuffer request,
            final long timeout) throws TimeoutException, InterruptedException, ExecutionException {
        int toWrite = request.limit();

        Future<Integer> fWrite = channel.write(request);
        Integer thisWrite = fWrite.get(timeout, TimeUnit.MILLISECONDS);
        toWrite -= thisWrite.intValue();

        while (toWrite > 0) {
            fWrite = channel.write(request);
            thisWrite = fWrite.get(timeout, TimeUnit.MILLISECONDS);
            toWrite -= thisWrite.intValue();
        }
    }


    private static boolean isRedirectStatus(final int httpResponseCode) {

        boolean isRedirect = false;

        switch (httpResponseCode) {
            case Constants.MULTIPLE_CHOICES:
            case Constants.MOVED_PERMANENTLY:
            case Constants.FOUND:
            case Constants.SEE_OTHER:
            case Constants.USE_PROXY:
            case Constants.TEMPORARY_REDIRECT:
                isRedirect = true;
            break;
            default:
            break;
        }

        return isRedirect;
    }


    private static ByteBuffer createProxyRequest(final String host, final int port) {
        final StringBuilder request = new StringBuilder();
        request.append("CONNECT ");
        request.append(host);
        request.append(':');
        request.append(port);

        request.append(" HTTP/1.1\r\nProxy-Connection: keep-alive\r\nConnection: keepalive\r\nHost: ");
        request.append(host);
        request.append(':');
        request.append(port);

        request.append("\r\n\r\n");

        final byte[] bytes = request.toString().getBytes(StandardCharsets.ISO_8859_1);
        return ByteBuffer.wrap(bytes);
    }


    protected void registerSession(final Object key, final WsSession wsSession) {

        if (!wsSession.isOpen()) {
            // The session was closed during onOpen. No need to register it.
            return;
        }
        synchronized (endPointSessionMapLock) {
            if (endpointSessionMap.size() == 0) {
                BackgroundProcessManager.getInstance().register(this);
            }
            Set<WsSession> wsSessions = endpointSessionMap.get(key);
            if (wsSessions == null) {
                wsSessions = new HashSet<>();
                endpointSessionMap.put(key, wsSessions);
            }
            wsSessions.add(wsSession);
        }
        sessions.put(wsSession, wsSession);
    }


    protected void unregisterSession(final Object key, final WsSession wsSession) {

        synchronized (endPointSessionMapLock) {
            final Set<WsSession> wsSessions = endpointSessionMap.get(key);
            if (wsSessions != null) {
                wsSessions.remove(wsSession);
                if (wsSessions.size() == 0) {
                    endpointSessionMap.remove(key);
                }
            }
            if (endpointSessionMap.size() == 0) {
                BackgroundProcessManager.getInstance().unregister(this);
            }
        }
        sessions.remove(wsSession);
    }


    public Set<Session> getOpenSessions(final Object key) {
        final HashSet<Session> result = new HashSet<>();
        synchronized (endPointSessionMapLock) {
            final Set<WsSession> sessions = endpointSessionMap.get(key);
            if (sessions != null) {
                result.addAll(sessions);
            }
        }
        return result;
    }


    private static Map<String, List<String>> createRequestHeaders(final String host, final int port,
            final ClientEndpointConfig clientEndpointConfiguration) {

        final Map<String, List<String>> headers = new HashMap<>();
        final List<Extension> extensions = clientEndpointConfiguration.getExtensions();
        final List<String> subProtocols = clientEndpointConfiguration.getPreferredSubprotocols();
        final Map<String, Object> userProperties = clientEndpointConfiguration.getUserProperties();

        if (userProperties.get(Constants.AUTHORIZATION_HEADER_NAME) != null) {
            final List<String> authValues = new ArrayList<>(1);
            authValues.add((String) userProperties.get(Constants.AUTHORIZATION_HEADER_NAME));
            headers.put(Constants.AUTHORIZATION_HEADER_NAME, authValues);
        }

        // Host header
        final List<String> hostValues = new ArrayList<>(1);
        if (port == -1) {
            hostValues.add(host);
        } else {
            hostValues.add(host + ':' + port);
        }

        headers.put(Constants.HOST_HEADER_NAME, hostValues);

        // Upgrade header
        final List<String> upgradeValues = new ArrayList<>(1);
        upgradeValues.add(Constants.UPGRADE_HEADER_VALUE);
        headers.put(Constants.UPGRADE_HEADER_NAME, upgradeValues);

        // Connection header
        final List<String> connectionValues = new ArrayList<>(1);
        connectionValues.add(Constants.CONNECTION_HEADER_VALUE);
        headers.put(Constants.CONNECTION_HEADER_NAME, connectionValues);

        // WebSocket version header
        final List<String> wsVersionValues = new ArrayList<>(1);
        wsVersionValues.add(Constants.WS_VERSION_HEADER_VALUE);
        headers.put(Constants.WS_VERSION_HEADER_NAME, wsVersionValues);

        // WebSocket key
        final List<String> wsKeyValues = new ArrayList<>(1);
        wsKeyValues.add(generateWsKeyValue());
        headers.put(Constants.WS_KEY_HEADER_NAME, wsKeyValues);

        // WebSocket sub-protocols
        if (subProtocols != null && subProtocols.size() > 0) {
            headers.put(Constants.WS_PROTOCOL_HEADER_NAME, subProtocols);
        }

        // WebSocket extensions
        if (extensions != null && extensions.size() > 0) {
            headers.put(Constants.WS_EXTENSIONS_HEADER_NAME,
                    generateExtensionHeaders(extensions));
        }

        return headers;
    }


    private static List<String> generateExtensionHeaders(final List<Extension> extensions) {
        final List<String> result = new ArrayList<>(extensions.size());
        for (final Extension extension : extensions) {
            final StringBuilder header = new StringBuilder();
            header.append(extension.getName());
            for (final Extension.Parameter param : extension.getParameters()) {
                header.append(';');
                header.append(param.getName());
                final String value = param.getValue();
                if (value != null && value.length() > 0) {
                    header.append('=');
                    header.append(value);
                }
            }
            result.add(header.toString());
        }
        return result;
    }


    private static String generateWsKeyValue() {
        final byte[] keyBytes = new byte[16];
        RANDOM.nextBytes(keyBytes);
        return Base64.encodeBase64String(keyBytes);
    }


    private static ByteBuffer createRequest(final URI uri, final Map<String, List<String>> reqHeaders) {
        ByteBuffer result = ByteBuffer.allocate(4 * 1024);

        // Request line
        result.put(GET_BYTES);
        if (null == uri.getPath() || "".equals(uri.getPath())) {
            result.put(ROOT_URI_BYTES);
        } else {
            result.put(uri.getRawPath().getBytes(StandardCharsets.ISO_8859_1));
        }
        final String query = uri.getRawQuery();
        if (query != null) {
            result.put((byte) '?');
            result.put(query.getBytes(StandardCharsets.ISO_8859_1));
        }
        result.put(HTTP_VERSION_BYTES);

        // Headers
        for (final Entry<String, List<String>> entry : reqHeaders.entrySet()) {
            result = addHeader(result, entry.getKey(), entry.getValue());
        }

        // Terminating CRLF
        result.put(CRLF);

        result.flip();

        return result;
    }


    private static ByteBuffer addHeader(ByteBuffer result, final String key, final List<String> values) {
        if (values.isEmpty()) {
            return result;
        }

        result = putWithExpand(result, key.getBytes(StandardCharsets.ISO_8859_1));
        result = putWithExpand(result, ": ".getBytes(StandardCharsets.ISO_8859_1));
        result = putWithExpand(result, StringUtils.join(values).getBytes(StandardCharsets.ISO_8859_1));
        result = putWithExpand(result, CRLF);

        return result;
    }


    private static ByteBuffer putWithExpand(ByteBuffer input, final byte[] bytes) {
        if (bytes.length > input.remaining()) {
            int newSize;
            if (bytes.length > input.capacity()) {
                newSize = 2 * bytes.length;
            } else {
                newSize = input.capacity() * 2;
            }
            final ByteBuffer expanded = ByteBuffer.allocate(newSize);
            input.flip();
            expanded.put(input);
            input = expanded;
        }
        return input.put(bytes);
    }


    /**
     * Process response, blocking until HTTP response has been fully received.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws DeploymentException
     * @throws TimeoutException
     */
    private HttpResponse processResponse(final ByteBuffer response,
            final AsyncChannelWrapper channel, final long timeout) throws InterruptedException,
            ExecutionException, DeploymentException, EOFException,
            TimeoutException {

        final Map<String, List<String>> headers = new CaseInsensitiveKeyMap<>();

        int status = 0;
        boolean readStatus = false;
        boolean readHeaders = false;
        String line = null;
        while (!readHeaders) {
            // On entering loop buffer will be empty and at the start of a new
            // loop the buffer will have been fully read.
            response.clear();
            // Blocking read
            final Future<Integer> read = channel.read(response);
            final Integer bytesRead = read.get(timeout, TimeUnit.MILLISECONDS);
            if (bytesRead.intValue() == -1) {
                throw new EOFException();
            }
            response.flip();
            while (response.hasRemaining() && !readHeaders) {
                if (line == null) {
                    line = readLine(response);
                } else {
                    line += readLine(response);
                }
                if ("\r\n".equals(line)) {
                    readHeaders = true;
                } else if (line.endsWith("\r\n")) {
                    if (readStatus) {
                        parseHeaders(line, headers);
                    } else {
                        status = parseStatus(line);
                        readStatus = true;
                    }
                    line = null;
                }
            }
        }

        return new HttpResponse(status, new WsHandshakeResponse(headers));
    }


    private int parseStatus(final String line) throws DeploymentException {
        // This client only understands HTTP 1.
        // RFC2616 is case specific
        final String[] parts = line.trim().split(" ");
        // CONNECT for proxy may return a 1.0 response
        if (parts.length < 2 || !("HTTP/1.0".equals(parts[0]) || "HTTP/1.1".equals(parts[0]))) {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.invalidStatus", line));
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (final NumberFormatException nfe) {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.invalidStatus", line));
        }
    }


    private void parseHeaders(final String line, final Map<String, List<String>> headers) {
        // Treat headers as single values by default.

        final int index = line.indexOf(':');
        if (index == -1) {
            log.warning(sm.getString("wsWebSocketContainer.invalidHeader", line));
            return;
        }
        // Header names are case insensitive so always use lower case
        final String headerName = line.substring(0, index).trim().toLowerCase(Locale.ENGLISH);
        // Multi-value headers are stored as a single header and the client is
        // expected to handle splitting into individual values
        final String headerValue = line.substring(index + 1).trim();

        List<String> values = headers.get(headerName);
        if (values == null) {
            values = new ArrayList<>(1);
            headers.put(headerName, values);
        }
        values.add(headerValue);
    }


    private String readLine(final ByteBuffer response) {
        // All ISO-8859-1
        final StringBuilder sb = new StringBuilder();

        char c = 0;
        while (response.hasRemaining()) {
            c = (char) response.get();
            sb.append(c);
            if (c == 10) {
                break;
            }
        }

        return sb.toString();
    }


    private SSLEngine createSSLEngine(final Map<String, Object> userProperties, final String host, final int port) throws DeploymentException {

        try {
            // See if a custom SSLContext has been provided
            SSLContext sslContext = (SSLContext) userProperties.get(Constants.SSL_CONTEXT_PROPERTY);

            if (sslContext == null) {
                // Create the SSL Context
                sslContext = SSLContext.getInstance("TLS");

                // Trust store
                final String sslTrustStoreValue = (String) userProperties.get(Constants.SSL_TRUSTSTORE_PROPERTY);
                if (sslTrustStoreValue != null) {
                    String sslTrustStorePwdValue = (String) userProperties.get(
                            Constants.SSL_TRUSTSTORE_PWD_PROPERTY);
                    if (sslTrustStorePwdValue == null) {
                        sslTrustStorePwdValue = Constants.SSL_TRUSTSTORE_PWD_DEFAULT;
                    }

                    final File keyStoreFile = new File(sslTrustStoreValue);
                    final KeyStore ks = KeyStore.getInstance("JKS");
                    try (InputStream is = new FileInputStream(keyStoreFile)) {
                        KeyStoreUtil.load(ks, is, sslTrustStorePwdValue.toCharArray());
                    }

                    final TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(ks);

                    sslContext.init(null, tmf.getTrustManagers(), null);
                } else {
                    sslContext.init(null, null, null);
                }
            }

            final SSLEngine engine = sslContext.createSSLEngine(host, port);

            final String sslProtocolsValue = (String) userProperties.get(Constants.SSL_PROTOCOLS_PROPERTY);
            if (sslProtocolsValue != null) {
                engine.setEnabledProtocols(sslProtocolsValue.split(","));
            }

            engine.setUseClientMode(true);

            // Enable host verification
            // Start with current settings (returns a copy)
            final SSLParameters sslParams = engine.getSSLParameters();
            // Use HTTPS since WebSocket starts over HTTP(S)
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            // Write the parameters back
            engine.setSSLParameters(sslParams);

            return engine;
        } catch (final Exception e) {
            throw new DeploymentException(sm.getString(
                    "wsWebSocketContainer.sslEngineFail"), e);
        }
    }


    @Override
    public long getDefaultMaxSessionIdleTimeout() {
        return defaultMaxSessionIdleTimeout;
    }


    @Override
    public void setDefaultMaxSessionIdleTimeout(final long timeout) {
        defaultMaxSessionIdleTimeout = timeout;
    }


    @Override
    public int getDefaultMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }


    @Override
    public void setDefaultMaxBinaryMessageBufferSize(final int max) {
        maxBinaryMessageBufferSize = max;
    }


    @Override
    public int getDefaultMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }


    @Override
    public void setDefaultMaxTextMessageBufferSize(final int max) {
        maxTextMessageBufferSize = max;
    }


    /**
     * {@inheritDoc}
     *
     * Currently, this implementation does not support any extensions.
     */
    @Override
    public Set<Extension> getInstalledExtensions() {
        return Collections.emptySet();
    }


    /**
     * {@inheritDoc}
     *
     * The default value for this implementation is -1.
     */
    @Override
    public long getDefaultAsyncSendTimeout() {
        return defaultAsyncTimeout;
    }


    /**
     * {@inheritDoc}
     *
     * The default value for this implementation is -1.
     */
    @Override
    public void setAsyncSendTimeout(final long timeout) {
        defaultAsyncTimeout = timeout;
    }


    /**
     * Cleans up the resources still in use by WebSocket sessions created from this container. This
     * includes closing sessions and cancelling {@link Future}s associated with blocking
     * read/writes.
     */
    public void destroy() {
        final CloseReason cr = new CloseReason(
                CloseCodes.GOING_AWAY, sm.getString("wsWebSocketContainer.shutdown"));

        for (final WsSession session : sessions.keySet()) {
            try {
                session.close(cr);
            } catch (final IOException ioe) {
                log.log(Level.FINEST, sm.getString("wsWebSocketContainer.sessionCloseFail", session.getId()), ioe);
            }
        }

        // Only unregister with AsyncChannelGroupUtil if this instance
        // registered with it
        if (asynchronousChannelGroup != null) {
            synchronized (asynchronousChannelGroupLock) {
                if (asynchronousChannelGroup != null) {
                    AsyncChannelGroupUtil.unregister();
                    asynchronousChannelGroup = null;
                }
            }
        }
    }


    private AsynchronousChannelGroup getAsynchronousChannelGroup() {
        // Use AsyncChannelGroupUtil to share a common group amongst all
        // WebSocket clients
        AsynchronousChannelGroup result = asynchronousChannelGroup;
        if (result == null) {
            synchronized (asynchronousChannelGroupLock) {
                if (asynchronousChannelGroup == null) {
                    asynchronousChannelGroup = AsyncChannelGroupUtil.register();
                }
                result = asynchronousChannelGroup;
            }
        }
        return result;
    }


    // ----------------------------------------------- BackgroundProcess methods

    @Override
    public void backgroundProcess() {
        // This method gets called once a second.
        backgroundProcessCount++;
        if (backgroundProcessCount >= processPeriod) {
            backgroundProcessCount = 0;

            for (final WsSession wsSession : sessions.keySet()) {
                wsSession.checkExpiration();
            }
        }

    }


    @Override
    public void setProcessPeriod(final int period) {
        processPeriod = period;
    }


    /**
     * {@inheritDoc}
     *
     * The default value is 10 which means session expirations are processed every 10 seconds.
     */
    @Override
    public int getProcessPeriod() {
        return processPeriod;
    }

    private static class HttpResponse {
        private final int status;
        private final HandshakeResponse handshakeResponse;


        public HttpResponse(final int status, final HandshakeResponse handshakeResponse) {
            this.status = status;
            this.handshakeResponse = handshakeResponse;
        }


        public int getStatus() {
            return status;
        }


        public HandshakeResponse getHandshakeResponse() {
            return handshakeResponse;
        }
    }
}
