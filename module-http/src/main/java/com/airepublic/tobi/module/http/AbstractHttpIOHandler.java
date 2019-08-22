package com.airepublic.tobi.module.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;

import javax.inject.Inject;

import com.airepublic.http.common.HttpStatus;
import com.airepublic.tobi.core.spi.ChannelAction;
import com.airepublic.tobi.core.spi.IAuthenticationService;
import com.airepublic.tobi.core.spi.IIOHandler;
import com.airepublic.tobi.core.spi.IRequest;
import com.airepublic.tobi.core.spi.IResponse;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Pair;

/**
 * A base implementation of the {@link IIOHandler} for all HTTP IO handlers. For responses that send
 * a HTTP 401 (Unauthorized) it will also delegate the call to the
 * {@link IHttpAuthorizationProvider} to request a login.
 * 
 * @author Torsten Oltmanns
 *
 */
public abstract class AbstractHttpIOHandler implements IIOHandler {
    private static final long serialVersionUID = 1L;
    @Inject
    private IServerSession session;
    @Inject
    private IAuthenticationService authorizationService;
    private HttpRequest request;


    /**
     * The default implementation will just return a {@link ChannelAction#CLOSE_INPUT}.
     * 
     * @param request the {@link Request} read from the incoming stream
     * @return {@link ChannelAction#CLOSE_INPUT}
     * @throws IOException if something goes wrong during processing
     */
    @Override
    public ChannelAction consume(final IRequest request) throws IOException {
        this.request = (HttpRequest) request;
        return ChannelAction.CLOSE_INPUT;
    }


    /**
     * The implementation tries to generate a {@link HttpResponse} by calling
     * {@link AbstractHttpIOHandler#getHttpResponse()} and writing the header- and body
     * {@link ByteBuffer} to the write-buffer-queue.
     * <p>
     * <b><i>IMPORTANT:</i></b> If the result of the call to
     * {@link AbstractHttpIOHandler#getHttpResponse()} is a <code>HTTP 401 (Unauthorized)</code> it
     * will call the {@link IHttpAuthorizationProvider#login(IServerSession)} method to produce the
     * response.
     * <p>
     * 
     * @throws IOException if something goes wrong during producing the {@link HttpResponse}
     */
    @Override
    public Pair<? extends IResponse, CompletionHandler<?, ?>> produce() throws IOException {
        final Pair<HttpResponse, CompletionHandler<?, ?>> response = getHttpResponse();

        if (response != null && response.getValue1() != null) {
            if (response.getValue1().getStatus() == HttpStatus.UNAUTHORIZED && session.getPrincipal() == null) {
                return authorizationService.login(session, response.getValue1());
            }
        }

        return response;
    }


    /**
     * Gets the current consumed {@link HttpRequest}.
     * 
     * @return the {@link HttpRequest}
     */
    protected HttpRequest getHttpRequest() {
        return request;
    }


    /**
     * The implementation will create and return a {@link HttpResponse} based on the
     * {@link HttpRequest}.
     * 
     * @return a {@link HttpResponse} with an associated {@link CompletionHandler}
     * @throws IOException if something goes wrong
     */
    protected abstract Pair<HttpResponse, CompletionHandler<?, ?>> getHttpResponse() throws IOException;


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
        if (t instanceof SecurityException) {
            System.out.println(t);
        }
        return ChannelAction.CLOSE_ALL;
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
