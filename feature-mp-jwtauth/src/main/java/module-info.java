import com.airepublic.tobi.feature.mp.jwtauth.JWTAuthorizationProvider;
import com.airepublic.tobi.module.http.IHttpAuthorizationProvider;

module com.airepublic.tobi.feature.mp.jwtauth {
    exports com.airepublic.tobi.feature.mp.jwtauth;

    requires com.airepublic.tobi.core.spi;
    requires com.airepublic.tobi.module.http;
    requires com.airepublic.http.common;
    requires com.airepublic.tobi.feature.mp.config;
    requires com.airepublic.logging.java;

    requires transitive microprofile.jwt.auth.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires jakarta.interceptor.api;
    requires java.annotation;

    requires jjwt;
    requires java.xml.bind;
    requires java.json;

    provides IHttpAuthorizationProvider with JWTAuthorizationProvider;

    opens com.airepublic.tobi.feature.mp.jwtauth;

}