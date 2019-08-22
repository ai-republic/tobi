package com.airepublic.tobi.feature.mp.jwtauth;

import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Interceptor for the {@link Authorized} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
@Authorized
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class AuthorizedInterceptor {
    @Inject
    private JsonWebToken jwt;


    /**
     * Intercepts the {@link Authorized} annotation and checks whether the JWT is permitted to call
     * the method.
     * 
     * @param context the {@link InvocationContext}
     * @return the method result
     * @throws SecurityException if the JWT is not allowed to access the method
     * @throws Throwable if anything happened in the called method
     */
    @AroundInvoke
    public Object intercept(final InvocationContext context) throws SecurityException, Throwable {
        if (jwt == null) {
            throw new SecurityException("Not authorized!");
        }

        final Authorized authorized = context.getMethod().getAnnotation(Authorized.class);
        final Set<String> groups = jwt.getGroups();

        // check if the JWT is allowed
        if (authorized.allow() != null) {
            final Set<String> allow = Set.of(authorized.allow());
            final boolean allowed = allow.stream().anyMatch(groups::contains);

            if (!allowed) {
                throw new SecurityException("Principal is not allowed!");
            }
        } else if (authorized.deny() != null) {
            // check if the JWT is denied
            final Set<String> deny = Set.of(authorized.allow());
            final boolean denied = deny.stream().anyMatch(groups::contains);

            if (denied) {
                throw new SecurityException("Principal is not allowed!");
            }
        }

        return context.proceed();
    }
}
