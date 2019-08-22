package com.airepublic.tobi.plugin.http.websocket;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import com.airepublic.http.common.BufferUtil;
import com.airepublic.http.common.pathmatcher.MappingResult;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.ChannelAction;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IRequest;
import com.airepublic.tobi.core.spi.IServerContext;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Pair;
import com.airepublic.tobi.module.http.AbstractHttpIOHandler;
import com.airepublic.tobi.module.http.HttpRequest;
import com.airepublic.tobi.module.http.HttpResponse;
import com.airepublic.tobi.plugin.http.websocket.server.UpgradeUtil;
import com.airepublic.tobi.plugin.http.websocket.server.WsFrameServer;
import com.airepublic.tobi.plugin.http.websocket.server.WsHttpUpgradeHandler;
import com.airepublic.tobi.plugin.http.websocket.server.WsRemoteEndpointImplServer;
import com.airepublic.tobi.plugin.http.websocket.server.WsServerContainer;

/**
 * The {@link IIOHandler} implementation for websocket IO.
 * 
 * @author Torsten Oltmanns
 *
 */
public class WebSocketIOHandler extends AbstractHttpIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private IServerContext serverContext;
    @Inject
    private IServerSession session;
    private static WsServerContainer webSocketContainer;
    private final AtomicBoolean handshakeDone = new AtomicBoolean(false);
    private final AtomicBoolean initWebSocketDone = new AtomicBoolean(false);
    private ServerEndpointConfig serverEndpointConfig;
    private final WsHttpUpgradeHandler upgradeHandler = new WsHttpUpgradeHandler();
    private final Queue<Pair<HttpResponse, CompletionHandler<?, ?>>> out = new ConcurrentLinkedQueue<>();


    /**
     * Initializes the websocket container.
     */
    @PostConstruct
    public void init() {
        webSocketContainer = (WsServerContainer) serverContext.getAttribute("websocket.container");
    }


    @Override
    public ChannelAction consume(final IRequest request) throws IOException {
        super.consume(request);

        // check if handshake request has been fully received and handshake has been processed
        final HttpRequest httpRequest = getHttpRequest();

        if (handshakeDone.get()) {
            try {
                if (serverEndpointConfig != null) {
                    final Set<Session> sessions = webSocketContainer.getOpenSessions(serverEndpointConfig.getPath());

                    if (!sessions.isEmpty()) {
                        final WsSession wsSession = (WsSession) sessions.iterator().next();
                        ((WsFrameServer) wsSession.getWsFrame()).onDataAvailable(httpRequest.getPayload());
                    }
                }
            } catch (final Exception e) {
                throw new IOException("Websocket request failed: " + httpRequest, e);
            }
        } else {
            session.getChannelProcessor().getChannel().keyFor(session.getChannelProcessor().getSelector()).interestOps(SelectionKey.OP_WRITE);
            session.getChannelProcessor().getSelector().wakeup();
        }

        return ChannelAction.KEEP_OPEN;
    }


    @Override
    protected Pair<HttpResponse, CompletionHandler<?, ?>> getHttpResponse() throws IOException {
        final HttpRequest httpRequest = getHttpRequest();

        if (!handshakeDone.get()) {
            try {
                final Pair<HttpResponse, CompletionHandler<?, ?>> result = new Pair<>(getHandshakeResponse(httpRequest), null);

                if (initWebSocketDone.compareAndSet(false, true)) {
                    initWebSocket(upgradeHandler);
                }

                return result;
            } catch (final Exception e) {
                throw new IOException("Error performing websocket handshake for request: " + httpRequest, e);
            }
        }

        if (!out.isEmpty()) {
            return out.poll();
        }

        return null;
    }


    /**
     * Perform the websocket upgrade handshake.
     * 
     * @param request the {@link HttpRequest}
     * @return the {@link HttpResponse}
     * @throws IOException if the handshake fails
     * @throws URISyntaxException if the request contains an invalid {@link URI}
     */
    private HttpResponse getHandshakeResponse(final HttpRequest request) throws IOException, URISyntaxException {
        if (handshakeDone.compareAndSet(false, true)) {
            try {
                final MappingResult<ServerEndpointConfig> mapping = webSocketContainer.getMapping().findMapping(request.getPath());
                serverEndpointConfig = mapping.getMappedObject();

                return UpgradeUtil.doUpgrade(webSocketContainer, request.getUri(), request.getHeaders(), request.getUserPrincipal(), serverEndpointConfig, mapping.getPathParams(), upgradeHandler);
            } catch (final Exception e) {
                handshakeDone.set(false);
                throw e;
            }
        }

        return null;
    }


    /**
     * Initializes the websocket after the upgrade.
     * 
     * @param handler the {@link WsHttpUpgradeHandler}
     */
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
                        try {
                            out.add(new Pair<>(new HttpResponse().withBody(BufferUtil.combineBuffers(buffers)), handler));

                            session.getChannelProcessor().getChannel().keyFor(session.getChannelProcessor().getSelector()).interestOpsOr(SelectionKey.OP_WRITE);
                            session.getChannelProcessor().getSelector().wakeup();

                        } catch (final Exception e) {
                            writeFailed(handler, e);
                        }
                    }
                }


                @Override
                protected void doClose() {
                    super.doClose();
                    session.close();
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


    /**
     * Creates a {@link Map} of the request query parameters.
     * 
     * @param query the request query part
     * @return the query parameters map
     */
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


    /**
     * Gets the {@link ServerEndpointConfig} from the specified annotated class.
     * 
     * @param pojoClass the class
     * @return the {@link ServerEndpointConfig}
     * @throws DeploymentException if the class is not annotated
     */
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
    @SuppressWarnings("unchecked")
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
        if (handshakeDone.get() && !(session.getChannelProcessor().getChannelEncoder() instanceof WebSocketEncoder)) {
            final WebSocketEncoder encoder = CDI.current().select(WebSocketEncoder.class).get();
            session.getChannelProcessor().setChannelEncoder(encoder);
            session.getChannelProcessor().getChannel().keyFor(session.getChannelProcessor().getSelector()).interestOps(SelectionKey.OP_READ);
        }

        if (handler != null) {
            ((CompletionHandler<Long, Void>) handler).completed(length, null);
        }

        return ChannelAction.KEEP_OPEN;
    }


    @Override
    @SuppressWarnings("unchecked")
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        if (handler != null) {
            ((CompletionHandler<Long, Void>) handler).failed(t, null);
        }

        if (!session.getChannel().isOpen()) {
            return ChannelAction.CLOSE_ALL;
        }
        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public ChannelAction onReadError(final Throwable t) {
        logger.log(Level.SEVERE, "Error not handled!", t);

        if (!session.getChannel().isOpen()) {
            return ChannelAction.CLOSE_ALL;
        }

        return ChannelAction.KEEP_OPEN;
    }


    @Override
    public void handleClosedInput() throws IOException {
    }


    @Override
    public void onSessionClose() {
    }
}
