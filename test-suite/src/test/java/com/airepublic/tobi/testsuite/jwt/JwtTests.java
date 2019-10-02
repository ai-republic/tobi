package com.airepublic.tobi.testsuite.jwt;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link JsonWebToken} and {@link ClaimValue} injection.
 * 
 * @author Torsten Oltmanns
 *
 */
public class JwtTests {
    @Inject
    private JsonWebToken jwt;
    @Inject
    @Claim(standard = Claims.iss)
    private ClaimValue<String> value;


    /**
     * Method to test {@link RolesAllowed} annotation in combination with the {@link JsonWebToken}.
     */
    @RolesAllowed("admin")
    public void doOnlyAdmin() {

    }


    /**
     * Method to test {@link DenyAll} annotation in combination with the {@link JsonWebToken}.
     */
    @DenyAll
    public void doNobody() {

    }


    /**
     * Method to test {@link PermitAll} annotation in combination with the {@link JsonWebToken}.
     */
    @PermitAll
    public void doEverybody() {

    }


    /**
     * Test the {@link JsonWebToken} and {@link ClaimValue} injection.
     */
    @Test
    public void testJwt() {
        final SeContainer container = SeContainerInitializer.newInstance().initialize();
        final JwtTests test = container.select(JwtTests.class).get();
        Assertions.assertNotNull(test.jwt);
        Assertions.assertNotNull(test.value);
    }

}
