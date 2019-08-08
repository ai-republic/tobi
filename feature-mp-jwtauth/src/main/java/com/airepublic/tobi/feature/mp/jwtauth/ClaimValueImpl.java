package com.airepublic.tobi.feature.mp.jwtauth;

import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;

/**
 * Implementation of the JWT {@link ClaimValue}.
 * 
 * @author Torsten Oltmanns
 *
 * @param <T> the claim value type
 */
public class ClaimValueImpl<T> implements ClaimValue<T> {
    private final String name;
    private final T value;


    /**
     * Constructor.
     * 
     * @param name the claim-name
     * @param value the value
     */
    public ClaimValueImpl(final String name, final T value) {
        this.name = name;
        this.value = value;
    }


    /**
     * Constructor.
     * 
     * @param claim the {@link Claims}
     * @param value the value
     */
    public ClaimValueImpl(final Claims claim, final T value) {
        this.name = claim.name();
        this.value = value;
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public T getValue() {
        return value;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
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
        final ClaimValueImpl<?> other = (ClaimValueImpl<?>) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "ClaimValueImpl [name=" + name + ", value=" + value + "]";
    }

}
