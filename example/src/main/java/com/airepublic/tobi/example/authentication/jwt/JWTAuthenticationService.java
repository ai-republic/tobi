package com.airepublic.tobi.example.authentication.jwt;

import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.Claims;

import com.airepublic.http.common.Headers;
import com.airepublic.http.common.HttpStatus;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.Attributes;
import com.airepublic.tobi.core.spi.IAuthenticationService;
import com.airepublic.tobi.core.spi.IResponse;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.Pair;
import com.airepublic.tobi.module.http.HttpResponse;
import com.airepublic.tobi.module.http.IHttpAuthorizationProvider;

/**
 * A JWT implementation of the {@link IHttpAuthorizationProvider}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class JWTAuthenticationService implements IAuthenticationService {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;


    @PostConstruct
    public void init() {
        logger.info("Using " + getClass().getSimpleName() + " as authorization-service");
    }


    @Override
    public Pair<IResponse, CompletionHandler<?, ?>> login(final IServerSession session, final IResponse response) {
        // perform BASIC login handshake
        final HttpResponse httpResponse = (HttpResponse) response;

        httpResponse.withStatus(HttpStatus.UNAUTHORIZED).withHeader(Headers.WWW_AUTHENTICATE, "Basic").withHeader(Headers.CONNECTION, "keep-alive");

        return new Pair<>(httpResponse, null);
    }


    @Override
    public Attributes authenticate(final String username, final String password) {
        final Attributes authorizationAttributes = new Attributes();
        authorizationAttributes.setAttribute(Claims.jti.name(), "1");
        authorizationAttributes.setAttribute(Claims.upn.name(), "default");
        authorizationAttributes.setAttribute(Claims.groups.name(), Set.of("ADMIN"));

        return authorizationAttributes;
    }

}
