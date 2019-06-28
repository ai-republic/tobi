
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.airepublic.microprofile.feature.mp.jwtauth.ClaimsSet;
import com.airepublic.microprofile.feature.mp.jwtauth.JWTUtil;

public class Configuration {
    @Inject
    private Logger logger;
    private JsonWebToken jwt;


    @Produces
    public JsonWebToken produce(final Config config) {
        final ClaimsSet claimSet = ClaimsSet.create("13", "Me", "AnySubject", "AnyUserPrincipalName", Set.of("AnyRole", "NoRole"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        claimSet.add("Hello", "World");

        final String pemFile = config.getValue("jwt.pemfile", String.class);

        if (pemFile != null) {
            try {
                jwt = JWTUtil.createJWT(Paths.get(pemFile), claimSet);
            } catch (final IOException e) {
                logger.log(Level.SEVERE, "Could not load PEM file from path: " + pemFile, e);
            }
        } else {
            final byte[] secretKey = "mysupersecretneverguessjwtincrediblekey".getBytes();
            jwt = JWTUtil.createJWT(secretKey, claimSet);
        }

        return jwt;
    }
}
