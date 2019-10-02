package com.airepublic.tobi.feature.cdi.owb;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.apache.webbeans.config.WebBeansFinder;

import com.airepublic.tobi.core.spi.ICDIServiceProvider;

/**
 * CDI provider for OpenWebBeans.
 * 
 * @author Torsten Oltmanns
 *
 */
public class CDIServiceProviderImpl implements ICDIServiceProvider {
    private static SeContainer seContainer = null;


    @Override
    public SeContainer getSeContainer() {
        if (seContainer == null) {
            WebBeansFinder.setSingletonService(new JavaServerDefaultSingletonService());
            seContainer = SeContainerInitializer.newInstance().setClassLoader(new ClassLoader("bootstrap", this.getClass().getClassLoader()) {
            }).initialize();
        }

        return seContainer;
    }

}
