package com.airepublic.tobi.feature.mp.jwtauth;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;

public class ClaimsSet implements Iterable<ClaimValue<?>> {
    private final Set<ClaimValue<?>> claims = new HashSet<>();


    public static ClaimsSet create(final String id, final String iss, final String sub, final String upn, final Set<String> groups, final LocalDateTime iat, final LocalDateTime exp) {
        final ClaimsSet map = new ClaimsSet();
        map.add(Claims.jti, id);
        map.add(Claims.iss, iss);
        map.add(Claims.sub, sub);
        map.add(Claims.iat, iat.atZone(ZoneId.systemDefault()).toEpochSecond());
        map.add(Claims.exp, exp.atZone(ZoneId.systemDefault()).toEpochSecond());
        map.add(Claims.upn, upn);
        map.add(Claims.groups, groups);
        // map.add(Claims.raw_token, rawToken);

        return map;
    }


    public static ClaimsSet create(final String id, final String iss, final String sub, final String upn, final Set<String> groups, final LocalDateTime iat, final LocalDateTime exp, final Map<Claims, Object> otherClaims) {
        final ClaimsSet map = create(id, iss, sub, upn, groups, iat, exp);

        otherClaims.entrySet().forEach(e -> map.add(e.getKey(), e.getValue()));

        return map;
    }


    private ClaimsSet() {
    }


    public <T> ClaimsSet add(final Claims claim, final T value) {
        add(new ClaimValueImpl<>(claim.name(), value));

        return this;
    }


    public <T> ClaimsSet add(final String claimName, final T value) {
        add(new ClaimValueImpl<>(claimName, value));

        return this;
    }


    public <T> ClaimsSet add(final ClaimValue<T> claimValue) {
        claims.add(claimValue);

        return this;
    }


    public boolean has(final Claims claim) {
        return has(claim.name());
    }


    public boolean has(final String claimName) {
        return claims.stream().filter(c -> c.getName().equals(claimName)).findFirst().isPresent();
    }


    public <T> ClaimValue<T> get(final Claims claim) {
        return get(claim.name());
    }


    @SuppressWarnings("unchecked")
    public <T> ClaimValue<T> get(final String claimName) {
        return (ClaimValue<T>) claims.stream().filter(c -> c.getName().equals(claimName)).findFirst().orElse(null);
    }


    @Override
    public Iterator<ClaimValue<?>> iterator() {
        return Collections.unmodifiableSet(claims).iterator();
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
        final ClaimsSet other = (ClaimsSet) obj;
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
        return "ClaimsMap [claims=" + claims + "]";
    }
}
