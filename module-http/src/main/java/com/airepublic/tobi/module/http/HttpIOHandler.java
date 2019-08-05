package com.airepublic.tobi.module.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpResponse;
import com.airepublic.http.common.HttpStatus;
import com.airepublic.tobi.core.spi.ChannelAction;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Request;

public class HttpIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    private IServerSession session;


    /**
     * Gets the current {@link IServerSession}.
     * 
     * @return the {@link IServerSession}
     */
    @Override
    public IServerSession getSession() {
        return session;
    }


    @Override
    public void setSession(final IServerSession session) {
        this.session = session;
    }


    /**
     * The default implementation will just send a {@link ChannelAction#CLOSE_INPUT}.
     * 
     * @param request the {@link Request} read from the incoming stream
     * @throws IOException if something goes wrong during processing
     */
    @Override
    public ChannelAction consume(final Request request) throws IOException {
        return ChannelAction.CLOSE_INPUT;
    }


    /**
     * The default implementation tries to generate a {@link HttpResponse} by calling
     * {@link HttpIOHandler#getHttpResponse()} and writing the header- and body {@link ByteBuffer}
     * to the write-buffer-queue.
     * 
     * @throws IOException if something goes wrong during producing the {@link HttpResponse}
     */
    @Override
    public void produce() throws IOException {
        final HttpResponse response = getHttpResponse();

        session.getChannelProcessor().addToWriteBuffer(response.getHeaderBuffer(), response.getBody());
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
     * The default implementation will return a HTTP 404 {@link HttpResponse}.
     * 
     * @return a HTTP 404 {@link HttpResponse}
     */
    protected HttpResponse getHttpResponse() {
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
    public void handleClosedInput() throws IOException {
        session.getChannelProcessor().getChannel().keyFor(session.getChannelProcessor().getSelector()).interestOpsOr(SelectionKey.OP_WRITE);
    }


    @Override
    public void onSessionClose() {
    }

}
