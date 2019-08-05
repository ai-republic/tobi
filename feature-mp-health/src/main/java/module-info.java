module com.airepublic.tobi.feature.mp.health {
    exports com.airepublic.tobi.feature.mp.health;

    requires transitive microprofile.health.api;
    requires com.airepublic.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires transitive java.json;

    opens com.airepublic.tobi.feature.mp.health;
}