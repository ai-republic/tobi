package com.airepublic.tobi.feature.mp.jwtauth;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.airepublic.http.common.Headers;
import com.airepublic.logging.java.LogLevel;
import com.airepublic.logging.java.LoggerConfig;
import com.airepublic.tobi.core.spi.Attributes;
import com.airepublic.tobi.core.spi.IAuthenticationService;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.module.http.HttpRequest;
import com.airepublic.tobi.module.http.IHttpAuthorizationProvider;

import io.jsonwebtoken.impl.TextCodec;

/**
 * Implementation of the {@link IHttpAuthorizationProvider} using JWT.
 * 
 * @author Torsten Oltmanns
 *
 */
public class JWTAuthorizationProvider implements IHttpAuthorizationProvider {
    @Inject
    @LoggerConfig(level = LogLevel.INFO)
    private Logger logger;
    @Inject
    private Config config;
    @Inject
    private IAuthenticationService authorizationService;


    @Override
    public void accept(final IServerSession session) throws IOException {
        produce(session);
    }


    @Produces
    public JsonWebToken produce(final IServerSession session) throws IOException {
        final HttpRequest httpRequest = (HttpRequest) session.getRequest();
        String authorization = null;

        if (httpRequest.getHeaders() != null) {
            authorization = httpRequest.getHeaders().getFirst(Headers.AUTHORIZATION);
        }

        // check if an authorization JWT bearer token exists, otherwise request authentication
        if (authorization != null) {
            // check if the session already has an authorized principal
            if (session.getPrincipal() != null) {
                // session has an authorized principal
                if (session.getPrincipal() instanceof JsonWebToken) {
                    // compare the JWT from the session to the JWT from the request
                    final JsonWebToken sessionToken = (JsonWebToken) session.getPrincipal();
                    // jwt = TextCodec.BASE64.decodeToString(jwt);

                    if (sessionToken.getRawToken().contentEquals(authorization)) {
                        return sessionToken;
                    } else {
                        return null;
                    }
                } else {
                    throw new SecurityException("Session has a principal set, but its not a JWT!");
                }
            } else {
                // otherwise this must be the authentication response after the handshake to
                // authenticate
                // check the authorization method
                final int idx = authorization.indexOf(' ');
                String authentication = null;

                if (idx != -1) {
                    authentication = TextCodec.BASE64.decodeToString(authorization.substring(idx + 1));
                }

                if (authentication != null && !authentication.isBlank()) {
                    // TODO get these from an authentication-service
                    final String[] usernamePassword = authentication.split(":");

                    if (usernamePassword == null || usernamePassword.length < 2) {
                        return null;
                    }

                    final Attributes authorizationAttributes = authorizationService.authenticate(usernamePassword[0], usernamePassword[1]);

                    final String id = authorizationAttributes.getString(Claims.jti.name());
                    final String iss = authorizationAttributes.getString(Claims.iss.name());
                    final String sub = authorizationAttributes.getString(Claims.sub.name());
                    final String upn = authorizationAttributes.getString(Claims.upn.name());
                    final Set<String> groups = authorizationAttributes.getAttribute(Claims.groups.name(), Set.class);
                    final LocalDateTime iat = LocalDateTime.now();
                    final LocalDateTime exp = iat.plusHours(1);
                    final ClaimsSet claims = ClaimsSet.create(id, iss, sub, upn, groups, iat, exp);

                    final String pemFile = config.getValue("jwt.pemfile", String.class);

                    if (pemFile != null) {
                        final JsonWebToken jsonWebToken = JWTUtil.createJWT(pemFile, claims);
                        session.setPrincipal(jsonWebToken);
                        httpRequest.getHeaders().set(Headers.AUTHORIZATION, jsonWebToken.getRawToken());
                        return jsonWebToken;
                    } else {
                        final String secretKey = config.getValue("jwt.secretKey", String.class);

                        if (secretKey != null) {
                            final JsonWebToken jsonWebToken = JWTUtil.createJWT(secretKey, claims);
                            session.setPrincipal(jsonWebToken);
                            httpRequest.getHeaders().set(Headers.AUTHORIZATION, jsonWebToken.getRawToken());
                            return jsonWebToken;
                        } else {
                            throw new IOException("Configuration is missing the JWT properties (jwt.pemfile or jwt.secretKey)!");
                        }
                    }
                }
            }
        }

        return null;
    }
}
