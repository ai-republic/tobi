import javax.enterprise.inject.spi.Extension;

import com.airepublic.microprofile.feature.mp.faulttolerance.AsynchronousCheckExtension;
import com.airepublic.microprofile.feature.mp.faulttolerance.FallbackAnnotationDecorator;

module com.airepublic.microprofile.feature.mp.faulttolerance {
    exports com.airepublic.microprofile.feature.mp.faulttolerance;

    requires transitive microprofile.fault.tolerance.api;

    requires cdi.api;
    requires java.annotation;
    requires javax.inject;
    requires javax.interceptor.api;

    provides Extension with AsynchronousCheckExtension, FallbackAnnotationDecorator;

    opens com.airepublic.microprofile.feature.mp.faulttolerance;

}