package com.airepublic.microprofile.plugin.http.websocket;

import java.io.EOFException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.plugin.http.websocket.server.UpgradeUtil;
import com.airepublic.microprofile.plugin.http.websocket.server.WsFrameServer;
import com.airepublic.microprofile.plugin.http.websocket.server.WsHttpUpgradeHandler;
import com.airepublic.microprofile.plugin.http.websocket.server.WsRemoteEndpointImplServer;
import com.airepublic.microprofile.plugin.http.websocket.server.WsServerContainer;
import com.airepublic.microprofile.util.http.common.AsyncHttpReader;
import com.airepublic.microprofile.util.http.common.HttpRequest;
import com.airepublic.microprofile.util.http.common.pathmatcher.MappingResult;

public class WebSocketIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private IServerContext serverContext;
    @Inject
    private IServerSession session;
    private static WsServerContainer webSocketContainer;
    private boolean handshakeDone = false;
    private final AsyncHttpReader httpRequestReader = new AsyncHttpReader();


    @PostConstruct
    public void init() {
        webSocketContainer = (WsServerContainer) serverContext.getAttribute("websocket.container");
    }


    @Override
    public ChannelAction consume(final ByteBuffer buffer) throws IOException {
        ChannelAction action = ChannelAction.KEEP_OPEN;

        // check if handshake request has been fully received and handshake has been processed
        if (!httpRequestReader.isFullyRead() && !handshakeDone) {
            try {
                if (httpRequestReader.receiveBuffer(buffer)) {
                    doHandshake();
                }
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Error receiving websocket upgrade request and handshake!", e);
                action = ChannelAction.CLOSE_ALL;
            }
        } else if (httpRequestReader.isFullyRead() && !handshakeDone) {
            // otherwise if request is fully read but handshake somehow not then do try
            // handshake now
            try {
                doHandshake();
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Error performing websocket handshake!", e);
                action = ChannelAction.CLOSE_ALL;
            }
        } else {
            try {
                System.out.println(buffer);
                final MappingResult<ServerEndpointConfig> mapping = webSocketContainer.getMapping().findMapping(httpRequestReader.getHttpRequest().getPath());

                if (mapping != null) {
                    final Set<Session> sessions = webSocketContainer.getOpenSessions(mapping.getMappedObject().getPath());

                    if (!sessions.isEmpty()) {
                        final WsSession wsSession = (WsSession) sessions.iterator().next();
                        ((WsFrameServer) wsSession.getWsFrame()).onDataAvailable(buffer);
                    }
                }
            } catch (final Exception e) {
                throw new IOException("Websocket request failed: " + httpRequestReader.getHttpRequest(), e);
            }
        }

        return action;
    }


    private void doHandshake() throws IOException, URISyntaxException {
        final HttpRequest request = httpRequestReader.getHttpRequest();
        final MappingResult<ServerEndpointConfig> mapping = webSocketContainer.getMapping().findMapping(request.getPath());
        final WsHttpUpgradeHandler handler = new WsHttpUpgradeHandler();
        final ByteBuffer response = UpgradeUtil.doUpgrade(webSocketContainer, request.getUri(), request.getHeaders(), request.getUserPrincipal(), mapping.getMappedObject(), mapping.getPathParams(), handler);
        session.addToWriteBuffer(response);
        handshakeDone = true;

        initWebSocket(handler);
    }


    public void initWebSocket(final WsHttpUpgradeHandler handler) {
        if (handler.getEp() == null) {
            throw new IllegalStateException("Upgrade handler not pre-initialized!");
        }

        final String httpSessionId = null;

        // Need to call onOpen using the web application's class loader
        // Create the frame using the application's class loader so it can pick
        // up application specific config from the ServerContainerImpl
        final Thread t = Thread.currentThread();
        final ClassLoader cl = t.getContextClassLoader();
        t.setContextClassLoader(Thread.currentThread().getContextClassLoader());

        try {
            final WsRemoteEndpointImplServer wsRemoteEndpointServer = new WsRemoteEndpointImplServer(webSocketContainer) {

                @Override
                protected void write(final boolean block, final long timeout, final TimeUnit unit, final CompletionHandler<?, ?> handler, final ByteBuffer... buffers) {
                    if (block) {
                        try {
                            final SocketChannel channel = session.getChannel();

                            if (channel.isOpen()) {
                                final long length = channel.write(buffers);
                                writeSuccessful(handler, length);
                            } else {
                                writeFailed(handler, new EOFException("Write channel is already closed!"));
                            }
                        } catch (final Exception e) {
                            writeFailed(handler, e);
                        }
                    } else {
                        session.addToWriteBuffer(handler, buffers);
                    }
                }
            };

            final HandshakeRequest handshakeRequest = handler.getHandshakeRequest();
            final WsSession wsSession = new WsSession(handler.getEp(), wsRemoteEndpointServer, webSocketContainer, handshakeRequest.getRequestURI(), handshakeRequest.getParameterMap(), handshakeRequest.getQueryString(), handshakeRequest.getUserPrincipal(), httpSessionId, handler.getNegotiatedExtensions(), handler.getSubProtocol(), handler.getPathParameters(), handler.isSecure(),
                    handler.getServerEndpointConfig());
            final WsFrameServer wsFrame = new WsFrameServer(wsSession, handler.getTransformation(), t.getContextClassLoader());
            // WsFrame adds the necessary final transformations. Copy the
            // completed transformation chain to the remote end point.
            wsRemoteEndpointServer.setTransformation(wsFrame.getTransformation());
            wsSession.setWsFrame(wsFrame);
            handler.getEp().onOpen(wsSession, handler.getServerEndpointConfig());
            webSocketContainer.registerSession(handler.getServerEndpointConfig().getPath(), wsSession);
        } catch (final DeploymentException e) {
            throw new IllegalArgumentException(e);
        } finally {
            t.setContextClassLoader(cl);
        }
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


    ServerEndpointConfig getAnnotatedEndpointConfig(final Class<?> pojoClass) throws DeploymentException {
        try {
            final ServerEndpoint annotation = pojoClass.getAnnotation(ServerEndpoint.class);

            if (annotation == null) {
                throw new DeploymentException("Server endpoint class isn't annotated with @ServerEndpoint annotation: " + pojoClass.getName());
            }

            final String path = annotation.value();

            // ServerEndpointConfig
            final Class<? extends Configurator> configuratorClass = annotation.configurator();
            Configurator configurator = null;

            if (!configuratorClass.equals(Configurator.class)) {
                try {
                    configurator = annotation.configurator().getConstructor().newInstance();
                } catch (final ReflectiveOperationException e) {
                    throw new DeploymentException("Could not instantiate server endpoint configurator (" + annotation.configurator().getName() + ") in class " + pojoClass.getClass().getName(), e);
                }
            }

            return ServerEndpointConfig.Builder.create(pojoClass, path).decoders(Arrays.asList(annotation.decoders())).encoders(Arrays.asList(annotation.encoders())).subprotocols(Arrays.asList(annotation.subprotocols())).configurator(configurator).build();
        } catch (final Exception e) {
            throw new DeploymentException("Deployment of " + pojoClass.getName() + " failed: ", e);
        }
    }


    @Override
    public void produce() throws IOException {
    }


    @SuppressWarnings("unchecked")
    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
        if (handler != null) {
            ((CompletionHandler<Long, Void>) handler).completed(length, null);
        }

        return ChannelAction.KEEP_OPEN;
    }


    @SuppressWarnings("unchecked")
    @Override
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        if (handler != null) {
            ((CompletionHandler<Long, Void>) handler).failed(t, null);
        }

        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public ChannelAction onReadError(final Throwable t) {
        logger.log(Level.SEVERE, "Error not handled!", t);
        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public void handleClosedInput() throws IOException {
    }


    @Override
    public void onSessionClose() {
    }


    public byte[] encode(final String mess) throws IOException {
        final byte[] rawData = mess.getBytes();

        int frameCount = 0;
        final byte[] frame = new byte[10];

        frame[0] = (byte) 129;

        if (rawData.length <= 125) {
            frame[1] = (byte) rawData.length;
            frameCount = 2;
        } else if (rawData.length >= 126 && rawData.length <= 65535) {
            frame[1] = (byte) 126;
            final int len = rawData.length;
            frame[2] = (byte) (len >> 8 & (byte) 255);
            frame[3] = (byte) (len & (byte) 255);
            frameCount = 4;
        } else {
            frame[1] = (byte) 127;
            final int len = rawData.length;
            frame[2] = (byte) (len >> 56 & (byte) 255);
            frame[3] = (byte) (len >> 48 & (byte) 255);
            frame[4] = (byte) (len >> 40 & (byte) 255);
            frame[5] = (byte) (len >> 32 & (byte) 255);
            frame[6] = (byte) (len >> 24 & (byte) 255);
            frame[7] = (byte) (len >> 16 & (byte) 255);
            frame[8] = (byte) (len >> 8 & (byte) 255);
            frame[9] = (byte) (len & (byte) 255);
            frameCount = 10;
        }

        final int bLength = frameCount + rawData.length;

        final byte[] reply = new byte[bLength];

        int bLim = 0;
        for (int i = 0; i < frameCount; i++) {
            reply[bLim] = frame[i];
            bLim++;
        }
        for (final byte element : rawData) {
            reply[bLim] = element;
            bLim++;
        }

        return reply;
    }

}
