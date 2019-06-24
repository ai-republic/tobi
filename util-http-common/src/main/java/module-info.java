module com.airepublic.microprofile.util.http.common {
    exports com.airepublic.microprofile.util.http.common;

    requires com.airepublic.microprofile.feature.mp.config;
    requires transitive com.airepublic.microprofile.core;

    opens com.airepublic.microprofile.util.http.common;
}