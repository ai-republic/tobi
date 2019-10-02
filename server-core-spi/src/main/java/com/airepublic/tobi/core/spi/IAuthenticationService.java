package com.airepublic.tobi.core.spi;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import javax.enterprise.context.SessionScoped;

/**
 * Service provider interface for the authentication service to assess whether a
 * {@link IServerSession} is allowed or not.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IAuthenticationService {

    /**
     * Produces a response as {@link ByteBuffer} to tell the client to perform a login.
     * 
     * @param session the {@link IServerSession}
     * @param response the {@link IResponse}
     * @return the {@link IResponse} to login and an optional {@link CompletionHandler}
     */
    Pair<IResponse, CompletionHandler<?, ?>> login(IServerSession session, IResponse response);


    /**
     * Authenticates the specified user with the password.
     * 
     * @param username the username
     * @param password the password
     * @return user attributes
     */
    Attributes authenticate(String username, String password);

}
