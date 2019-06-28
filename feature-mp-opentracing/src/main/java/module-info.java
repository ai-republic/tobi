module com.airepublic.microprofile.feature.mp.opentracing {
    requires transitive com.airepublic.microprofile.feature.mp.config;

    requires microprofile.opentracing.api;

    requires cdi.api;
    requires java.annotation;
    requires javax.inject;

    requires smallrye.opentracing;
}