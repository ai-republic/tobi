module com.airepublic.microprofile.feature.mp.metrics {
    requires com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.metrics.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires jakarta.interceptor.api;

    requires smallrye.metrics;
}