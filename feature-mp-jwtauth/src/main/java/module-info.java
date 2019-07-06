module com.airepublic.microprofile.feature.mp.jwtauth {
    exports com.airepublic.microprofile.feature.mp.jwtauth;

    requires com.airepublic.microprofile.feature.mp.config;
    requires com.airepublic.microprofile.feature.logging.java;

    requires transitive microprofile.jwt.auth.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires jjwt;
    requires java.xml.bind;
    requires java.json;

    opens com.airepublic.microprofile.feature.mp.jwtauth;

}