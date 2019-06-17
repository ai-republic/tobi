package com.airepublic.microprofile.module.http.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.ChannelAction;
import com.airepublic.microprofile.core.IServicePlugin;

/**
 * This class is a default implementation of the {@link AbstractIOHandler} for the HTTP
 * protocol.<br/>
 * All {@link IServicePlugin} implementations for the HTTP protocol should use the
 * {@link IServicePluginHttp} interface and can use this class as a base needing mostly only to
 * implement the {@link AbstractHttpIOHandler#deploy()} and
 * {@link AbstractHttpIOHandler#getHttpResponse()} methods.
 * 
 * @author Torsten Oltmanns
 *
 */
public abstract class AbstractHttpIOHandler extends AbstractIOHandler {
    private final AsyncHttpRequestReader requestReader = new AsyncHttpRequestReader();


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
    protected ChannelAction consume(final ByteBuffer buffer) throws IOException {
        return requestReader.receiveRequestBuffer(buffer);
    }


    /**
     * The default implementation tries to generate a {@link HttpResponse} by calling
     * {@link AbstractHttpIOHandler#getHttpResponse()} and writing the header- and body
     * {@link ByteBuffer} to the write-buffer-queue.
     * 
     * @throws IOException if something goes wrong during producing the {@link HttpResponse}
     */
    @Override
    protected void produce() throws IOException {
        final HttpResponse response = getHttpResponse();

        getSession().addToWriteBuffer(response.getHeaderBuffer(), response.getBody());
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
    protected ChannelAction writeSuccessful(final CompletionHandler<?, ?> handler, final long length) {
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
    protected ChannelAction writeFailed(final CompletionHandler<?, ?> handler, final Throwable t) {
        return ChannelAction.CLOSE_ALL;
    }


    /**
     * The default implementation signals to close the connection.
     * 
     * @param t the exception
     * @return {@link ChannelAction#CLOSE_ALL}
     */
    @Override
    protected ChannelAction onReadError(final Throwable t) {
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
     * The default implementation will return a HTTP 404 {@link HttpResponse}.
     * 
     * @return a HTTP 404 {@link HttpResponse}
     */
    public HttpResponse getHttpResponse() {
        final Headers headers = new Headers();
        headers.add(Headers.CONTENT_TYPE, "text/plain");
        final ByteBuffer buffer = ByteBuffer.wrap("The page requested was not found.".getBytes());
        final HttpResponse response = new HttpResponse(HttpStatus.NOT_FOUND, headers, buffer);

        return response;
    }


    /**
     * The default implementation will tell the connection that it is ready to write.
     * 
     * @throws IOException if something goes wrong
     */
    @Override
    protected void handleClosedInput() throws IOException {
        getSession().getKey().interestOpsOr(SelectionKey.OP_WRITE);
    }

}
