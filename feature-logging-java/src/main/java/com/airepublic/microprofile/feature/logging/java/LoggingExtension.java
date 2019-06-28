package com.airepublic.microprofile.feature.logging.java;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public class LoggingExtension implements Extension {
    public void configureAllLoggers(@Observes final AfterBeanDiscovery abd, final BeanManager bm) {
        LogManager.getLogManager().getLoggerNames().asIterator().forEachRemaining(name -> Stream.of(LogManager.getLogManager().getLogger(name).getHandlers()).forEach(h -> h.setFormatter(new DefaultFormatter())));
        Stream.of(Logger.getGlobal().getHandlers()).forEach(h -> h.setFormatter(new DefaultFormatter()));
        Stream.of(Logger.getAnonymousLogger().getHandlers()).forEach(h -> h.setFormatter(new DefaultFormatter()));
    }

}
