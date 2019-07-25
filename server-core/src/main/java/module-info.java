import com.airepublic.microprofile.core.ChannelProcessor;
import com.airepublic.microprofile.core.ServerContext;
import com.airepublic.microprofile.core.ServerSession;
import com.airepublic.microprofile.core.spi.ICDIServiceProvider;
import com.airepublic.microprofile.core.spi.IChannelProcessor;
import com.airepublic.microprofile.core.spi.IServerContext;
import com.airepublic.microprofile.core.spi.IServerModule;
import com.airepublic.microprofile.core.spi.IServerSession;
import com.airepublic.microprofile.core.spi.IServicePlugin;

module com.airepublic.microprofile.core {
    exports com.airepublic.microprofile.core;
    exports com.airepublic.microprofile.core.util;

    requires com.airepublic.microprofile.core.spi;
    requires com.airepublic.microprofile.feature.mp.config;
    requires com.airepublic.microprofile.feature.mp.faulttolerance;
    requires com.airepublic.microprofile.feature.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires jakarta.interceptor.api;

    requires java.net.http;

    opens com.airepublic.microprofile.core;

    uses IServerModule;
    uses IServicePlugin;
    uses ICDIServiceProvider;

    provides IServerSession with ServerSession;
    provides IServerContext with ServerContext;
    provides IChannelProcessor with ChannelProcessor;
}