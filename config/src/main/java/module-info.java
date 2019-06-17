import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.airepublic.microprofile.config.ConfigProviderResolverImpl;

module com.airepublic.microprofile.config {
    exports com.airepublic.microprofile.config;

    requires transitive microprofile.config.api;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires transitive jdk.unsupported;
    requires transitive weld.se.shaded;

    uses ConfigProviderResolver;
    uses ConfigSource;
    uses Converter;

    provides ConfigProviderResolver with ConfigProviderResolverImpl;

    opens com.airepublic.microprofile.config;
}