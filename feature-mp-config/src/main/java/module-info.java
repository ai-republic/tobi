import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.airepublic.microprofile.feature.mp.config.ConfigProviderResolverImpl;

module com.airepublic.microprofile.feature.mp.config {
    exports com.airepublic.microprofile.feature.mp.config;

    requires transitive microprofile.config.api;
    requires java.annotation;
    requires cdi.api;
    requires javax.inject;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    uses ConfigProviderResolver;
    uses ConfigSource;
    uses Converter;

    provides ConfigProviderResolver with ConfigProviderResolverImpl;

    opens com.airepublic.microprofile.feature.mp.config;
}