package com.airepublic.microprofile.feature.mp.jwtauth;

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

public class Test {
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


    public static void main(final String[] args) {
        final SeContainer container = SeContainerInitializer.newInstance().initialize();
        final Test test = container.select(Test.class).get();
        System.out.println(test.jwt);
        System.out.println(test.value);
    }

}
