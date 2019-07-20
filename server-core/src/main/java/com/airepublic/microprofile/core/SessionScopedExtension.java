package com.airepublic.microprofile.core;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

public class SessionScopedExtension implements Extension {
    public void registerSessionScoped(@Observes final AfterBeanDiscovery abd) {
        abd.addContext(new RequestScopedContext());
        abd.addContext(new SessionScopedContext());
    }
}
