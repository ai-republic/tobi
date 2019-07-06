import javax.enterprise.inject.spi.Extension;

import com.airepublic.microprofile.feature.logging.java.LoggingExtension;

module com.airepublic.microprofile.feature.logging.java {
    exports com.airepublic.microprofile.feature.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires transitive java.logging;

    opens com.airepublic.microprofile.feature.logging.java;

    provides Extension with LoggingExtension;
}