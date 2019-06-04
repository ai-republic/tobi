import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.airepublic.microprofile.config.ConfigProviderResolverImpl;

module com.airepublic.microprofile.config {
    exports com.airepublic.microprofile.config;

    requires transitive microprofile.config.api;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    provides ConfigProviderResolver with ConfigProviderResolverImpl;

    opens com.airepublic.microprofile.config;
}