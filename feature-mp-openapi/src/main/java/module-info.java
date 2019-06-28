module com.airepublic.microprofile.feature.mp.openapi {
    requires transitive com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.openapi.api;

    requires cdi.api;
    requires java.annotation;
    requires javax.inject;

    requires smallrye.open.api;
}