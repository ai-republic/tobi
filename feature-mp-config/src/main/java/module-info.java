import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.airepublic.microprofile.feature.mp.config.ConfigProviderResolverImpl;

module com.airepublic.microprofile.feature.mp.config {
    exports com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.config.api;

    requires com.airepublic.microprofile.feature.logging.java;
    requires cdi.api;
    requires java.annotation;
    requires javax.inject;

    uses ConfigProviderResolver;
    uses ConfigSource;
    uses Converter;

    provides ConfigProviderResolver with ConfigProviderResolverImpl;

    opens com.airepublic.microprofile.feature.mp.config;
}