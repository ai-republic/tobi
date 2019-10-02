package com.airepublic.tobi.feature.cdi.weld;

import javax.enterprise.inject.se.SeContainer;

import org.jboss.weld.environment.se.Weld;

import com.airepublic.tobi.core.spi.ICDIServiceProvider;

/**
 * CDI provider for WELD.
 * 
 * @author Torsten Oltmanns
 *
 */
public class CDIServiceProviderImpl implements ICDIServiceProvider {
    private static SeContainer seContainer = null;


    @Override
    public SeContainer getSeContainer() {
        if (seContainer == null) {
            seContainer = new Weld("javaserver").initialize();
        }

        return seContainer;
    }
}
