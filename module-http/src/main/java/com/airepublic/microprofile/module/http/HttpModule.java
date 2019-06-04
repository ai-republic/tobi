package com.airepublic.microprofile.module.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.BufferUtil;
import com.airepublic.microprofile.core.DetermineStatus;
import com.airepublic.microprofile.core.IServerModule;
import com.airepublic.microprofile.core.Pair;
import com.airepublic.microprofile.core.ServerContext;
import com.airepublic.microprofile.core.ServerSession;

public class HttpModule implements IServerModule {
    private final static Logger LOG = LoggerFactory.getLogger(HttpModule.class);
    private final Map<String, Class<? extends AbstractIOHandler>> mappings = new ConcurrentHashMap<>();


    @Override
    public String getName() {
        return "Http module";
    }


    @Override
    public void initModule(final Config config, final ServerContext serverContext) throws IOException {
    }


    public void addMapping(final String path, final Class<? extends AbstractIOHandler> ioHandlerClass) {
        mappings.put(path, ioHandlerClass);
    }


    protected Class<? extends AbstractIOHandler> findMapping(final String path) {
        return mappings.get(path);
    }


    @Override
    public Pair<DetermineStatus, AbstractIOHandler> determineHandlerClass(final ByteBuffer buffer, final ServerSession session) throws IOException {

        String path = null;

        // mark buffer to reset it after read to leave it untouched for handler
        buffer.mark();
        final String line = BufferUtil.readLine(buffer);
        buffer.reset();

        // check for the URI request line
        if (line != null) {
            final int startIdx = line.indexOf(" ");

            if (startIdx != -1) {
                final int endIdx = line.indexOf(" ", startIdx + 1);

                if (endIdx != -1) {
                    path = line.substring(startIdx, endIdx).strip();

                    // strip trailing slash if there is one
                    if (path != null && path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }
                } else {
                    throw new IOException(line + " does not contain valid URI");
                }
            } else {
                throw new IOException(line + " does not contain valid URI");
            }
        } else {
            return new Pair<>(DetermineStatus.NEED_MORE_DATA, null);
        }

        // check if there is a SocketHandler for the path
        Class<? extends AbstractIOHandler> handlerClass = null;

        if (path != null) {
            handlerClass = findMapping(path);
        }

        AbstractIOHandler handler = null;

        try {
            // if no handler was mapped, use default HttpSocketHandler
            if (handlerClass == null) {
                return new Pair<>(DetermineStatus.FALSE, null);
            } else if (session.getServerContext().getCdiContainer() != null) {
                handler = session.getServerContext().getCdiContainer().select(handlerClass).get();
            } else {
                handler = handlerClass.getConstructor().newInstance();
            }

            handler.init(session);
        } catch (final Exception e) {
            LOG.error("Could not instantiate handler: " + handlerClass.getName(), e);
            throw new IOException("Could not initialize handler: " + handlerClass, e);
        }

        LOG.info("Using " + handler.getClass().getName() + " for request: " + path);

        return new Pair<>(DetermineStatus.TRUE, handler);

    }

}
