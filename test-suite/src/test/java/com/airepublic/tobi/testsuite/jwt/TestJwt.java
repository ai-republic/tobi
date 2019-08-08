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

public class TestJwt {
    @Inject
    private JsonWebToken jwt;
    @Inject
    @Claim(standard = Claims.iss)
    private ClaimValue<String> value;


    @RolesAllowed("admin")
    public void doOnlyAdmin() {

    }


    @DenyAll
    public void doNobody() {

    }


    @PermitAll
    public void doEverybody() {

    }


    @Test
    public void testJwt() {
        final SeContainer container = SeContainerInitializer.newInstance().initialize();
        final TestJwt test = container.select(TestJwt.class).get();
        Assertions.assertNotNull(test.jwt);
        Assertions.assertNotNull(test.value);
    }

}
