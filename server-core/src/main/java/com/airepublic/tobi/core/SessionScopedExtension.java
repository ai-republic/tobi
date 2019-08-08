package com.airepublic.tobi.core;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * CDI {@link Extension} to add {@link SessionScoped} context.
 * 
 * @author Torsten Oltmanns
 * 
 *
 */
public class SessionScopedExtension implements Extension {

    /**
     * + Register the {@link SessionScoped} context.
     * 
     * @param event the {@link AfterBeanDiscovery} event
     */
    public void registerSessionScoped(@Observes final AfterBeanDiscovery event) {
        event.addContext(new RequestScopedContext());
        event.addContext(new SessionScopedContext());
    }
}
