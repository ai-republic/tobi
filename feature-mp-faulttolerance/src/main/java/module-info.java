import javax.enterprise.inject.spi.Extension;

import com.airepublic.tobi.feature.mp.faulttolerance.AsynchronousCheckExtension;
import com.airepublic.tobi.feature.mp.faulttolerance.FallbackAnnotationDecorator;

module com.airepublic.tobi.feature.mp.faulttolerance {
    exports com.airepublic.tobi.feature.mp.faulttolerance;

    requires transitive microprofile.fault.tolerance.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires jakarta.interceptor.api;

    provides Extension with AsynchronousCheckExtension, FallbackAnnotationDecorator;

    opens com.airepublic.tobi.feature.mp.faulttolerance;

}