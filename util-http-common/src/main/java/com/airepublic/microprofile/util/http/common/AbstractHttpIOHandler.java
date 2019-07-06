package com.airepublic.microprofile.util.http.common;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import com.airepublic.microprofile.core.spi.ChannelAction;
import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.IServicePlugin;

/**
 * This class is a default implementation of the {@link IIOHandler} for the HTTP protocol.<br/>
 * All {@link IServicePlugin} implementations for the HTTP protocol should use the
 * {@link IServicePluginHttp} interface and can use this class as a base needing mostly only to
 * implement the {@link AbstractHttpIOHandler#deploy()} and
 * {@link AbstractHttpIOHandler#getHttpResponse()} methods.
 * 
 * @author Torsten Oltmanns
 *
 */
@SessionScoped
public abstract class AbstractHttpIOHandler implements IIOHandler, Serializable {
    private static final long serialVersionUID = 1L;
    private final AsyncHttpRequestReader requestReader = new AsyncHttpRequestReader();
    @Inject
    private IServerSession session;


    /**
     * The default implementation forwards the buffer to the {@link AsyncHttpRequestReader} to parse
     * the {@link HttpRequest}.<br/>
     * Once it has read the {@link HttpRequest} completely it will send a
     * {@link ChannelAction#CLOSE_INPUT} otherwise a {@link ChannelAction#KEEP_OPEN}.
     * 
     * @param buffer the {@link ByteBuffer} read from the incoming stream
     * @throws IOException if something goes wrong during processing of the {@link ByteBuffer}
     */
    @Override
    public ChannelAction consume(final ByteBuffer buffer) throws IOException {
        return requestReader.receiveRequestBuffer(buffer) ? ChannelAction.CLOSE_INPUT : ChannelAction.KEEP_OPEN;
    }


    /**
     * The default implementation tries to generate a {@link HttpResponse} by calling
     * {@link AbstractHttpIOHandler#getHttpResponse()} and writing the header- and body
     * {@link ByteBuffer} to the write-buffer-queue.
     * 
     * @throws IOException if something goes wrong during producing the {@link HttpResponse}
     */
    @Override
    public void produce() throws IOException {
        final HttpResponse response = getHttpResponse();

        session.addToWriteBuffer(response.getHeaderBuffer(), response.getBody());
    }


    /**
     * The default implementation ignores the {@link CompletionHandler} and signals to close the
     * connection.
     * 
     * @param handler a handler (ignored)
     * @param length the amount of bytes written
     * @return {@link ChannelAction#CLOSE_ALL}
     */
    @Override
    public ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
        return ChannelAction.CLOSE_ALL;
    }


    /**
     * The default implementation ignores the {@link CompletionHandler} and signals to close the
     * connection.
     * 
     * @param handler a handler (ignored)
     * @param t the exception
     * @return {@link ChannelAction#CLOSE_ALL}
     */
    @Override
    public ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    /**
     * The default implementation signals to close the connection.
     * 
     * @param t the exception
     * @return {@link ChannelAction#CLOSE_ALL}
     */
    @Override
    public ChannelAction onReadError(final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    /**
     * Gets the {@link HttpRequest} if it has been fully received otherwise null.
     * 
     * @return the {@link HttpRequest} if it has been fully received otherwise null
     * @throws IOException if something goes wrong during processing of the {@link HttpRequest}
     */
    public HttpRequest getHttpRequest() throws IOException {
        return requestReader.getHttpRequest();
    }


    /**
     * The implementation should produce a {@link HttpResponse} according to the
     * {@link HttpRequest}.
     * 
     * @return a HTTP 404 {@link HttpResponse}
     */
    public abstract HttpResponse getHttpResponse();


    /**
     * The default implementation will tell the connection that it is ready to write.
     * 
     * @throws IOException if something goes wrong
     */
    @Override
    public void handleClosedInput() throws IOException {
        session.getSelectionKey().interestOpsOr(SelectionKey.OP_WRITE);
    }

}
