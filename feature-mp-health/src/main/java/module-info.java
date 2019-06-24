module com.airepublic.microprofile.feature.mp.health {
    exports com.airepublic.microprofile.feature.mp.health;

    requires transitive microprofile.health.api;

    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires cdi.api;
    requires java.annotation;
    requires javax.inject;

    requires transitive java.json;

    opens com.airepublic.microprofile.feature.mp.health;
}