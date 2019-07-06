module com.airepublic.microprofile.core.spi {
    exports com.airepublic.microprofile.core.spi;

    requires com.airepublic.microprofile.feature.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;

}