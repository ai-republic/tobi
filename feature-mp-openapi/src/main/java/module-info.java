module com.airepublic.microprofile.feature.mp.openapi {
    requires transitive com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.openapi.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires smallrye.open.api;
}