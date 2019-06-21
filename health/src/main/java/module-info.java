module com.airepublic.microprofile.health {
    exports com.airepublic.microprofile.health;

    requires transitive microprofile.health.api;

    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires transitive weld.se.shaded;
    requires transitive jdk.unsupported;

    requires transitive java.json;

    opens com.airepublic.microprofile.health;
}