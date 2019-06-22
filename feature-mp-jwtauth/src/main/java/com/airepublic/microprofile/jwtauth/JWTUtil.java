package com.airepublic.microprofile.jwtauth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JWTUtil {
    private final static Logger LOG = LoggerFactory.getLogger(JWTUtil.class);


    public static JsonWebToken createJWT(final Path keyFile, final ClaimsSet claims) throws IOException {
        return createJWT(loadPrivateKey(keyFile), claims);

    }


    public static JsonWebToken createJWT(final byte[] secretKey, final ClaimsSet claims) {
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
        final Key signingKey = new SecretKeySpec(secretKey, signatureAlgorithm.getJcaName());

        return createJWT(signingKey, claims);
    }


    public static JsonWebToken createJWT(final Key signingKey, final ClaimsSet claims) {
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
        final Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        final JwtBuilder builder = Jwts.builder();

        claims.forEach(c -> builder.claim(c.getName(), c.getValue()));

        builder.setNotBefore(now).signWith(signatureAlgorithm, signingKey);

        final String jwt = builder.compact();

        return new JsonWebTokenImpl(jwt, signingKey);
    }


    private static Key loadPrivateKey(final Path keyFile) throws IOException {
        String privateKey;
        try {
            privateKey = Files.readString(keyFile);
        } catch (final IOException e) {
            LOG.error("Private key file could not be read: ", e);
            throw new RuntimeException(e);
        }

        // Remove the "BEGIN" and "END" lines, as well as any whitespace
        String pkcs8Pem = privateKey.toString();
        pkcs8Pem = pkcs8Pem.replace("-----BEGIN RSA PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replace("-----END RSA PRIVATE KEY-----", "");
        // pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");
        pkcs8Pem = pkcs8Pem.replaceAll("\\n", "");
        pkcs8Pem = pkcs8Pem.replaceAll("\\r", "");
        pkcs8Pem = pkcs8Pem.replaceAll(" ", "");

        // Base64 decode the result
        final byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(pkcs8Pem);

        // extract the private key
        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf;

        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (final NoSuchAlgorithmException e) {
            LOG.error("No RSA algorythm found!", e);
            throw new RuntimeException(e);
        }

        Key privKey;

        try {
            privKey = kf.generatePrivate(keySpec);
        } catch (final InvalidKeySpecException e) {
            LOG.error("Invalid private key", e);
            throw new RuntimeException(e);
        }

        return privKey;

    }


    public static void main(final String[] args) throws IOException {
        final byte[] secretKey = "mysupersecretneverguessjwtincrediblekey".getBytes();

        // with claims map
        final ClaimsSet claimSet = ClaimsSet.create("13", "Me", "AnySubject", "AnyUserPrincipalName", Set.of("AnyRole", "NoRole"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        claimSet.add("Hello", "World");

        final JsonWebToken jwt1 = createJWT(secretKey, claimSet);
        System.out.println("Create JWT: " + jwt1);
        jwt1.getClaimNames().forEach(name -> System.out.println(name + " -> " + jwt1.getClaim(name)));

        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS512;
        final Key signingKey = new SecretKeySpec(secretKey, signatureAlgorithm.getJcaName());
        final JsonWebToken jwt2 = createJWT(signingKey, claimSet);
        System.out.println("Create JWT: " + jwt2);
        jwt2.getClaimNames().forEach(name -> System.out.println(name + " -> " + jwt2.getClaim(name)));

        final JsonWebToken jwt3 = createJWT(Path.of("D:/Development/test.pem"), claimSet);
        System.out.println("Create JWT: " + jwt3);
        jwt3.getClaimNames().forEach(name -> System.out.println(name + " -> " + jwt3.getClaim(name)));
    }

}
