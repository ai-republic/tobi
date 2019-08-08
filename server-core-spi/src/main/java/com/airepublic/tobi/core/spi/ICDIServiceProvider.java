package com.airepublic.tobi.core.spi;

import javax.enterprise.inject.se.SeContainer;

/**
 * CDI provider service interface.
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
