package com.airepublic.microprofile.core.spi;

import javax.enterprise.inject.se.SeContainer;

public interface ICDIServiceProvider {
    SeContainer getSeContainer();
}
