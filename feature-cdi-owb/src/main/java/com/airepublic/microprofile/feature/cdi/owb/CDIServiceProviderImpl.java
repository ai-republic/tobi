package com.airepublic.microprofile.feature.cdi.owb;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.apache.webbeans.config.WebBeansFinder;

import com.airepublic.microprofile.core.spi.ICDIServiceProvider;

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


    @Produces
    public SeContainer produceSeContainer() {
        return getSeContainer();
    }
}
