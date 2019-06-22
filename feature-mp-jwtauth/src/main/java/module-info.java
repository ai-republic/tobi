module com.airepublic.microprofile.jwtauth {
    exports com.airepublic.microprofile.jwtauth;

    requires com.airepublic.microprofile.config;

    requires transitive microprofile.jwt.auth.api;

    requires org.slf4j;
    requires ch.qos.logback.classic;

    // requires cdi.api;
    // requires java.annotation;
    // requires javax.inject;
    // requires openwebbeans.se;
    // requires openwebbeans.spi;
    // requires openwebbeans.impl;
    requires transitive jdk.unsupported;
    requires transitive weld.se.shaded;

    requires jjwt;
    requires java.xml.bind;
    requires java.json;

    opens com.airepublic.microprofile.jwtauth;

}