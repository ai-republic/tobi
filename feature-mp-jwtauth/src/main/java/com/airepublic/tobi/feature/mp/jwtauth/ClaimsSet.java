package com.airepublic.tobi.feature.mp.jwtauth;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;

/**
 * An {@link Iterable} of {@link ClaimValue}s.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ClaimsSet implements Iterable<ClaimValue<?>> {
    private final Set<ClaimValue<?>> claims = new HashSet<>();


    /**
     * Creates a {@link ClaimsSet}.
     * 
     * @param id the id
     * @param iss the issuer
     * @param sub the subject
     * @param upn the unique principal name
     * @param groups the groups
     * @param iat the issue date
     * @param exp the expiry date
     * @return the created {@link ClaimsSet}
     */
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


    /**
     * Creates a {@link ClaimsSet}.
     * 
     * @param id the id
     * @param iss the issuer
     * @param sub the subject
     * @param upn the unique principal name
     * @param groups the groups
     * @param iat the issue date
     * @param exp the expiry date
     * @param otherClaims a map containing other {@link Claim}s to be added to the {@link ClaimsSet}
     * @return the created {@link ClaimsSet}
     */
    public static ClaimsSet create(final String id, final String iss, final String sub, final String upn, final Set<String> groups, final LocalDateTime iat, final LocalDateTime exp, final Map<Claims, Object> otherClaims) {
        final ClaimsSet map = create(id, iss, sub, upn, groups, iat, exp);

        otherClaims.entrySet().forEach(e -> map.add(e.getKey(), e.getValue()));

        return map;
    }


    /**
     * Creates a {@link ClaimsSet} based on the contents of the map. The map must contain at least
     * the required claims.
     * 
     * @param claims the map with claims
     * @return the {@link ClaimsSet}
     */
    public static ClaimsSet create(final Map<String, Object> claims) {
        final String[] requiredNames = { Claims.jti.name(), Claims.iss.name(), Claims.sub.name(), Claims.iat.name(), Claims.exp.name(), Claims.upn.name(), Claims.groups.name() };

        if (Stream.of(requiredNames).allMatch(claims::containsKey)) {
            final ClaimsSet map = new ClaimsSet();
            claims.entrySet().forEach(e -> map.add(e.getKey(), e.getValue()));

            return map;
        } else {
            throw new IllegalArgumentException("Map does not contain required claims!");
        }

    }


    /**
     * Constructor.
     */
    private ClaimsSet() {
    }


    /**
     * Adds the {@link Claims} and its value.
     * 
     * @param <T> the Claim value type
     * @param claim the {@link Claims}
     * @param value the value
     * @return this {@link ClaimsSet}
     */
    public <T> ClaimsSet add(final Claims claim, final T value) {
        add(new ClaimValueImpl<>(claim.name(), value));

        return this;
    }


    /**
     * Adds the claim name and its value.
     * 
     * @param <T> the claim value type
     * @param claimName the name of the claim
     * @param value the value
     * @return this {@link ClaimsSet}
     */
    public <T> ClaimsSet add(final String claimName, final T value) {
        add(new ClaimValueImpl<>(claimName, value));

        return this;
    }


    /**
     * Adds the {@link ClaimValue}.
     * 
     * @param <T> the Claim value type
     * @param claimValue the {@link ClaimValue}
     * @return this {@link ClaimsSet}
     */
    public <T> ClaimsSet add(final ClaimValue<T> claimValue) {
        claims.add(claimValue);

        return this;
    }


    /**
     * Checks whether the specified {@link Claims} name is registered.
     * 
     * @param claim the {@link Claims}
     * @return true if registered in this set
     */
    public boolean has(final Claims claim) {
        return has(claim.name());
    }


    /**
     * Checks whether the specified claim-name is registered.
     * 
     * @param claimName the claim-name
     * @return true if registered in this set
     */
    public boolean has(final String claimName) {
        return claims.stream().filter(c -> c.getName().equals(claimName)).findFirst().isPresent();
    }


    /**
     * Gets the {@link ClaimValue} for the specified {@link Claims}.
     * 
     * @param <T> the claim value type
     * @param claim the {@link Claims}
     * @return the {@link ClaimValue}
     */
    public <T> ClaimValue<T> get(final Claims claim) {
        return get(claim.name());
    }


    /**
     * Gets the {@link ClaimValue} for the specified claim-name.
     * 
     * @param <T> the claim value type
     * @param claimName the claim-name
     * @return the {@link ClaimValue}
     */
    @SuppressWarnings("unchecked")
    public <T> ClaimValue<T> get(final String claimName) {
        return (ClaimValue<T>) claims.stream().filter(c -> c.getName().equals(claimName)).findFirst().orElse(null);
    }


    /**
     * Gets an unmodifiable {@link Iterator} of all claims.
     * 
     * @return the {@link Iterator}
     */
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
