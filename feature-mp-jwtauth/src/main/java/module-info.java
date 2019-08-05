module com.airepublic.tobi.feature.mp.jwtauth {
    exports com.airepublic.tobi.feature.mp.jwtauth;

    requires com.airepublic.tobi.feature.mp.config;
    requires com.airepublic.logging.java;

    requires transitive microprofile.jwt.auth.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires jjwt;
    requires java.xml.bind;
    requires java.json;

    opens com.airepublic.tobi.feature.mp.jwtauth;

}