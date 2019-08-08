package com.airepublic.tobi.core.spi;

import javax.enterprise.inject.se.SeContainer;

/**
 * CDI provider service provider interface which is used to load custom configured CDI providers.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface ICDIServiceProvider {

    /**
     * Gets a custom configured CDI {@link SeContainer}.
     * 
     * @return the {@link SeContainer}
     */
    SeContainer getSeContainer();
}
