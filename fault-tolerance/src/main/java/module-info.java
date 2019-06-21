import javax.enterprise.inject.spi.Extension;

import com.airepublic.microprofile.faulttolerance.FallbackBindingAnnotator;

module com.airepublic.microprofile.faulttolerance {
    exports com.airepublic.microprofile.faulttolerance;

    requires org.slf4j;
    requires ch.qos.logback.classic;

    // requires cdi.api;
    // requires java.annotation;
    // requires javax.inject;
    // requires javax.interceptor.api;
    // requires openwebbeans.se;
    // requires openwebbeans.spi;
    // requires openwebbeans.impl;
    requires transitive weld.se.shaded;

    requires jdk.unsupported;
    requires transitive microprofile.fault.tolerance.api;

    provides Extension with FallbackBindingAnnotator;

    opens com.airepublic.microprofile.faulttolerance;

}