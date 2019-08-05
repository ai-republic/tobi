module com.airepublic.tobi.feature.mp.openapi {
    requires transitive com.airepublic.tobi.feature.mp.config;

    requires transitive microprofile.openapi.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires smallrye.open.api;
}