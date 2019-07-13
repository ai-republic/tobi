package com.airepublic.microprofile.plugin.http.sse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.sse.SseEventSink;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.feature.logging.java.LogLevel;
import com.airepublic.microprofile.feature.logging.java.LoggerConfig;
import com.airepublic.microprofile.util.http.common.AsyncHttpReader;
import com.airepublic.microprofile.util.http.common.Headers;
import com.airepublic.microprofile.util.http.common.HttpRequest;
import com.airepublic.microprofile.util.http.common.HttpResponse;
import com.airepublic.microprofile.util.http.common.HttpStatus;
import com.airepublic.microprofile.util.http.common.pathmatcher.MappingResult;
import com.airepublic.microprofile.util.http.common.pathmatcher.PathMapping;

@SessionScoped
public class SseOutboundIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    private final AsyncHttpReader httpReader = new AsyncHttpReader();
    @Inject
    private IServerSession session;
    @Inject
    private SseEventSink sseEventSink;
    private final AtomicBoolean isHandshakeRead = new AtomicBoolean(false);
    private Object serviceObject;
    private Method serviceMethod;
    private long delayInMs = 0L;
    private long times = 0L;
    private long maxTimes = -1L;


    @Override
    public ChannelAction consume(final ByteBuffer buffer) throws IOException {
        if (!isHandshakeRead.get()) {
            if (httpReader.receiveBuffer(buffer)) {
                isHandshakeRead.set(true);

                try {
                    doHandshake(httpReader.getHttpRequest());
                    return ChannelAction.CLOSE_INPUT;
                } catch (final URISyntaxException e) {
                    throw new IOException("Error in request URI syntax!", e);
                }
            }
        }

        throw new IllegalCallerException("Input should be closed for any further calls!");
    }


    @SuppressWarnings("unchecked")
    private void doHandshake(final HttpRequest httpRequest) throws IOException, URISyntaxException {
        final PathMapping<Method> sseMethodMapping = session.getAttribute(SsePlugin.SSE_METHOD_MAPPING, PathMapping.class);
        final MappingResult<Method> mappingResult = sseMethodMapping.findMapping(httpRequest.getPath());

        if (mappingResult == null || mappingResult.getMappedObject() == null) {
            throw new IOException("URI path " + httpRequest.getPath() + " could not be mapped to a SSE method!");
        }

        serviceMethod = mappingResult.getMappedObject();
        serviceObject = CDI.current().select(serviceMethod.getDeclaringClass()).get();

        if (serviceMethod.isAnnotationPresent(SseRepeat.class)) {
            final SseRepeat annotation = serviceMethod.getAnnotation(SseRepeat.class);
            delayInMs = annotation.unit().toMillis(annotation.delay());
            maxTimes = annotation.maxTimes();
        }

        final Headers headers = new Headers();
        headers.add(Headers.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
        headers.add(Headers.CONNECTION, "keep-alive");
        headers.add(Headers.CACHE_CONTROL, "no-cache");

        final HttpResponse response = new HttpResponse(HttpStatus.OK, headers);

        session.addToWriteBuffer(response.getHeaderBuffer());
    }


    @Override
    public void produce() throws IOException {
        try {
            final Parameter[] params = serviceMethod.getParameters();
            final Object[] objs = new Object[params.length];

            if (params.length > 0) {
                int i = 0;

                for (final Parameter param : params) {
                    if (SseEventSink.class.isAssignableFrom(param.getType())) {
                        objs[i] = sseEventSink;
                    } else {
                        throw new IllegalArgumentException("SSE event produce method defines parameter " + param.getName() + " which is not allowed. Only SseEventSink is allowed.");
                    }

                    i++;
                }
            }

            serviceMethod.invoke(serviceObject, objs);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Could not invoke SSE outbound producer method: " + serviceMethod, e);
        }
    }


    @Override
    public ChannelAction onReadError(final Throwable t) {
        return ChannelAction.CLOSE_INPUT;
    }


    @Override
    public void handleClosedInput() throws IOException {
    }


    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {

        if (maxTimes == -1 || times < maxTimes) {
            times++;

            try {
                Thread.sleep(delayInMs);
                session.getSelectionKey().interestOps(SelectionKey.OP_WRITE);
                session.getSelectionKey().selector().wakeup();
                return ChannelAction.CLOSE_INPUT;
            } catch (final InterruptedException e) {
                logger.log(Level.WARNING, "SSE for outbound events was interrupted!", e);
                return ChannelAction.CLOSE_ALL;
            }
        } else {
            return ChannelAction.CLOSE_ALL;
        }
    }


    @Override
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    @Override
    public void onSessionClose() {
        sseEventSink.close();
    }
}
