module com.airepublic.tobi.feature.mp.opentracing {
    requires transitive com.airepublic.tobi.feature.mp.config;

    requires microprofile.opentracing.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires smallrye.opentracing;
}