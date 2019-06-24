import javax.enterprise.inject.spi.Extension;

import com.airepublic.microprofile.feature.mp.faulttolerance.FallbackAnnotationDecorator;

module com.airepublic.microprofile.feature.mp.faulttolerance {
    exports com.airepublic.microprofile.feature.mp.faulttolerance;

    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires cdi.api;
    requires java.annotation;
    requires javax.inject;
    requires javax.interceptor.api;

    requires transitive microprofile.fault.tolerance.api;

    provides Extension with FallbackAnnotationDecorator;

    opens com.airepublic.microprofile.feature.mp.faulttolerance;

}