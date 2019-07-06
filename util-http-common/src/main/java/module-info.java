module com.airepublic.microprofile.util.http.common {
    exports com.airepublic.microprofile.util.http.common;
    exports com.airepublic.microprofile.util.http.common.pathmatcher;

    requires com.airepublic.microprofile.core.spi;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;

    opens com.airepublic.microprofile.util.http.common;
}