module com.airepublic.microprofile.feature.mp.jwtauth {
    exports com.airepublic.microprofile.feature.mp.jwtauth;

    requires com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.jwt.auth.api;

    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires cdi.api;
    requires java.annotation;
    requires javax.inject;

    requires jjwt;
    requires java.xml.bind;
    requires java.json;

    opens com.airepublic.microprofile.feature.mp.jwtauth;

}