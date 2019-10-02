import com.airepublic.tobi.core.ChannelProcessor;
import com.airepublic.tobi.core.ServerContext;
import com.airepublic.tobi.core.ServerSession;
import com.airepublic.tobi.core.spi.ICDIServiceProvider;
import com.airepublic.tobi.core.spi.IChannelProcessor;
import com.airepublic.tobi.core.spi.IServerContext;
import com.airepublic.tobi.core.spi.IServerModule;
import com.airepublic.tobi.core.spi.IServerSession;
import com.airepublic.tobi.core.spi.IServicePlugin;

module com.airepublic.tobi.core {
    exports com.airepublic.tobi.core;

    requires com.airepublic.tobi.core.spi;
    requires com.airepublic.tobi.feature.mp.config;
    requires com.airepublic.tobi.feature.mp.faulttolerance;
    requires com.airepublic.logging.java;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires jakarta.interceptor.api;

    opens com.airepublic.tobi.core;

    uses IServerModule;
    uses IServicePlugin;
    uses ICDIServiceProvider;

    provides IServerSession with ServerSession;
    provides IServerContext with ServerContext;
    provides IChannelProcessor with ChannelProcessor;

}