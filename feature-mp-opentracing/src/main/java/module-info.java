module com.airepublic.microprofile.feature.mp.opentracing {
    requires transitive com.airepublic.microprofile.feature.mp.config;

    requires microprofile.opentracing.api;

    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires cdi.api;
    requires java.annotation;
    requires javax.inject;

    requires smallrye.opentracing;
}