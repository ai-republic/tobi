import javax.enterprise.inject.spi.Extension;

import com.airepublic.tobi.core.spi.SessionScopedExtension;

module com.airepublic.tobi.core.spi {
    exports com.airepublic.tobi.core.spi;

    requires com.airepublic.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;

    provides Extension with SessionScopedExtension;

}