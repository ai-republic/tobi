module com.airepublic.microprofile.feature.mp.health {
    exports com.airepublic.microprofile.feature.mp.health;

    requires transitive microprofile.health.api;
    requires com.airepublic.microprofile.feature.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires transitive java.json;

    opens com.airepublic.microprofile.feature.mp.health;
}