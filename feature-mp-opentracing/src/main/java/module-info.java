module com.airepublic.microprofile.feature.mp.opentracing {
    requires transitive com.airepublic.microprofile.feature.mp.config;

    requires microprofile.opentracing.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    requires smallrye.opentracing;
}