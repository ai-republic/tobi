import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.airepublic.tobi.feature.mp.config.ConfigProviderResolverImpl;

module com.airepublic.tobi.feature.mp.config {
    exports com.airepublic.tobi.feature.mp.config;

    requires com.airepublic.logging.java;
    requires transitive microprofile.config.api;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;

    uses ConfigProviderResolver;
    uses ConfigSource;
    uses Converter;

    provides ConfigProviderResolver with ConfigProviderResolverImpl;

    opens com.airepublic.tobi.feature.mp.config;
}