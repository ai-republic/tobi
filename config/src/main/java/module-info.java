import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.airepublic.microprofile.config.ConfigProviderResolverImpl;
import com.airepublic.microprofile.config.EnvironmentConfigSource;
import com.airepublic.microprofile.config.MicroprofileDefaultConfigSource;
import com.airepublic.microprofile.config.SystemPropertiesConfigSource;

module com.airepublic.microprofile.config {
    exports com.airepublic.microprofile.config;

    requires transitive microprofile.config.api;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    uses ConfigProviderResolver;
    uses ConfigSource;
    uses Converter;

    provides ConfigProviderResolver with ConfigProviderResolverImpl;
    provides ConfigSource with EnvironmentConfigSource, SystemPropertiesConfigSource, MicroprofileDefaultConfigSource;

    opens com.airepublic.microprofile.config;
}