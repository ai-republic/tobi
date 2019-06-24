module com.airepublic.microprofile.feature.mp.metrics {
    requires com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.metrics.api;

    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires cdi.api;
    requires java.annotation;
    requires javax.inject;
    requires javax.interceptor.api;

    requires smallrye.metrics;
}