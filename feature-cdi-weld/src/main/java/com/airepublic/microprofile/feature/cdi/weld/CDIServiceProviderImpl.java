package com.airepublic.microprofile.feature.cdi.weld;

import javax.enterprise.inject.se.SeContainer;

import org.jboss.weld.environment.se.Weld;

import com.airepublic.microprofile.core.spi.ICDIServiceProvider;

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
