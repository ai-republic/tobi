package com.airepublic.tobi.feature.mp.jwtauth;

import java.security.Key;
import java.util.Collections;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class JsonWebTokenImpl implements JsonWebToken {
    private final Claims claims;


    public JsonWebTokenImpl(final String jwt, final String secretKey) {
        claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt).getBody();
        claims.put(org.eclipse.microprofile.jwt.Claims.raw_token.name(), jwt);
    }


    public JsonWebTokenImpl(final String jwt, final byte[] secretKey) {
        claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt).getBody();
        claims.put(org.eclipse.microprofile.jwt.Claims.raw_token.name(), jwt);
    }


    public JsonWebTokenImpl(final String jwt, final Key secretKey) {

        claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt).getBody();
        claims.put(org.eclipse.microprofile.jwt.Claims.raw_token.name(), jwt);
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


    @SuppressWarnings("unchecked")
    @Override
    public <T> T getClaim(final String claimName) {
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
