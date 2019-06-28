module com.airepublic.microprofile.feature.mp.metrics {
    requires com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.metrics.api;

    requires cdi.api;
    requires java.annotation;
    requires javax.inject;
    requires javax.interceptor.api;

    requires smallrye.metrics;
}