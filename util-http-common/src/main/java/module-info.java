import com.airepublic.microprofile.core.spi.IRequest;
import com.airepublic.microprofile.util.http.common.HttpRequest;

module com.airepublic.microprofile.util.http.common {
    exports com.airepublic.microprofile.util.http.common;
    exports com.airepublic.microprofile.util.http.common.pathmatcher;

    requires com.airepublic.microprofile.core.spi;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.logging;

    provides IRequest with HttpRequest;

    opens com.airepublic.microprofile.util.http.common;
    opens com.airepublic.microprofile.util.http.common.pathmatcher;
}