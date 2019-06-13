package com.airepublic.microprofile.javaserver.boot;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;

import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microprofile.jwtauth.ClaimsSet;
import com.airepublic.microprofile.jwtauth.JWTUtil;

public class Configuration {
    private final static Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static Config config;
    private JsonWebToken jwt;


    @Produces
    public Config produceConfig() {
        if (config == null) {
            config = ConfigProviderResolver.instance().getConfig();
        }

        return config;
    }


    @Produces
    public JsonWebToken produce(final Config config) {
        final ClaimsSet claimSet = ClaimsSet.create("13", "Me", "AnySubject", "AnyUserPrincipalName", Set.of("AnyRole", "NoRole"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        claimSet.add("Hello", "World");

        final String pemFile = config.getValue("jwt.pemfile", String.class);

        if (pemFile != null) {
            try {
                jwt = JWTUtil.createJWT(Paths.get(pemFile), claimSet);
            } catch (final IOException e) {
                LOG.error("Could not load PEM file from path: " + pemFile, e);
            }
        } else {
            final byte[] secretKey = "mysupersecretneverguessjwtincrediblekey".getBytes();
            jwt = JWTUtil.createJWT(secretKey, claimSet);
        }

        return jwt;
    }

}
