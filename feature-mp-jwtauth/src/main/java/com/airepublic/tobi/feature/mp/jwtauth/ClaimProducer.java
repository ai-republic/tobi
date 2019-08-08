package com.airepublic.tobi.feature.mp.jwtauth;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * CDI producer to inject {@link Claim}s.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ClaimProducer {

    /**
     * Produces the {@link ClaimValue} of the JWT for {@link InjectionPoint}.
     * 
     * @param <T> the {@link ClaimValue} value type
     * @param jwt the JWT
     * @param ip the {@link InjectionPoint}
     * @return the {@link ClaimValue}
     */
    @SuppressWarnings("unchecked")
    @Produces
    @Claim
    public <T> ClaimValue<T> getClaimValue(final JsonWebToken jwt, final InjectionPoint ip) {

        if (ip.getAnnotated().isAnnotationPresent(Claim.class)) {
            final Claim claim = ip.getAnnotated().getAnnotations(Claim.class).stream().findFirst().get();

            return new ClaimValueImpl<>(claim.standard().name(), (T) jwt.getClaim(claim.standard().name()));
        }

        return null;
    }
}
