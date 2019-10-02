module com.airepublic.tobi.feature.mp.metrics {
    requires com.airepublic.tobi.feature.mp.config;

    requires transitive microprofile.metrics.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires jakarta.interceptor.api;

    requires smallrye.metrics;
}