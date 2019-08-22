package com.airepublic.tobi.feature.mp.jwtauth;

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Implementation of the {@link JsonWebToken}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class JsonWebTokenImpl implements JsonWebToken {
    private Claims claims;


    /**
     * Constructor.
     * 
     * @param jwt the JWT string
     * @param secretKey the secret key
     */
    public JsonWebTokenImpl(final String jwt, final String secretKey) {
        refresh(jwt, secretKey);
    }


    /**
     * Constructor.
     * 
     * @param jwt the JWT string
     * @param secretKey the secret key
     */
    public JsonWebTokenImpl(final String jwt, final byte[] secretKey) {
        refresh(jwt, secretKey);
    }


    /**
     * Constructor.
     * 
     * @param jwt the JWT string
     * @param secretKey the secret key
     */
    public JsonWebTokenImpl(final String jwt, final Key secretKey) {
        refresh(jwt, secretKey);
    }


    /**
     * Refresh the JWT.
     * 
     * @param jwt the JWT
     * @param secretKey the secret key
     */
    void refresh(final String jwt, final String secretKey) {
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
        final Key signingKey = new SecretKeySpec(secretKey.getBytes(), signatureAlgorithm.getJcaName());

        final JwtParser parser = Jwts.parser().setSigningKey(signingKey);
        createClaims(parser, jwt);
    }


    /**
     * Refresh the JWT.
     * 
     * @param jwt the JWT
     * @param secretKey the secret key
     */
    void refresh(final String jwt, final byte[] secretKey) {
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
        final Key signingKey = new SecretKeySpec(secretKey, signatureAlgorithm.getJcaName());

        final JwtParser parser = Jwts.parser().setSigningKey(signingKey);
        createClaims(parser, jwt);
    }


    /**
     * Refresh the JWT.
     * 
     * @param jwt the JWT
     * @param secretKey the secret key
     */
    void refresh(final String jwt, final Key secretKey) {
        final JwtParser parser = Jwts.parser().setSigningKey(secretKey);
        createClaims(parser, jwt);
    }


    /**
     * Create the claims instance.
     * 
     * @param parser the {@link JwtParser}
     * @param jwt the JWT
     */
    private void createClaims(final JwtParser parser, final String jwt) {
        claims = parser.parseClaimsJws(jwt).getBody();
        claims.put(org.eclipse.microprofile.jwt.Claims.raw_token.name(), "Bearer " + jwt);
    }


    @Override
    public String getName() {
        String principalName = claims.get(org.eclipse.microprofile.jwt.Claims.upn.name(), String.class);

        if (principalName == null) {
            principalName = claims.get(org.eclipse.microprofile.jwt.Claims.preferred_username.name(), String.class);
        }

        return principalName;
    }


    @Override
    public Set<String> getClaimNames() {
        return Collections.unmodifiableSet(claims.keySet());
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T getClaim(final String claimName) {
        if (claimName.contentEquals(org.eclipse.microprofile.jwt.Claims.groups.name())) {
            final Collection<String> groups = (Collection<String>) claims.get(claimName);
            return (T) Set.copyOf(groups);
        }
        return (T) claims.get(claimName);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (claims == null ? 0 : claims.hashCode());
        return result;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JsonWebTokenImpl other = (JsonWebTokenImpl) obj;
        if (claims == null) {
            if (other.claims != null) {
                return false;
            }
        } else if (!claims.equals(other.claims)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "JsonWebTokenImpl [claims=" + claims + "]";
    }

}
