package com.airepublic.microprofile.feature.logging.java;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Produces a {@link Logger} for injection which can be configured using the {@link LoggerConfig}
 * annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class LoggerProducer {

    @Produces
    public Logger produceLogger(final InjectionPoint injectionPoint) {
        final SerializableLogger logger = new SerializableLogger(injectionPoint.getMember().getDeclaringClass().getName());
        logger.setLevel(Level.ALL);
        final Formatter formatter = new DefaultFormatter();
        Stream.of(logger.getHandlers()).forEach(h -> h.setFormatter(formatter));
        LogManager.getLogManager().addLogger(logger);

        return logger;
    }


    @Produces
    @LoggerConfig
    public Logger produceConfiguredLogger(final InjectionPoint injectionPoint) {
        if (((Field) injectionPoint.getMember()).isAnnotationPresent(LoggerConfig.class)) {
            final LoggerConfig config = ((Field) injectionPoint.getMember()).getAnnotation(LoggerConfig.class);
            SerializableLogger logger;

            if (!config.name().isBlank()) {
                logger = new SerializableLogger(config.name());
            } else {
                logger = new SerializableLogger(injectionPoint.getMember().getDeclaringClass().getName());
            }

            Handler handler = null;

            try {
                handler = config.handler().getConstructor().newInstance();
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Configured handler '" + config.handler() + "' could not be instantiated via default constructor! Defaulting to ConsoleHandler.", e);
                handler = new ConsoleHandler();
            }

            Stream.of(logger.getHandlers()).forEach(logger::removeHandler);
            logger.addHandler(handler);
            logger.setLevel(config.level().getLevel());

            String format = null;
            Formatter formatter = null;

            if (config.format() != null && !config.format().isBlank()) {
                format = config.format();
            }

            if (config.formatter() != null) {
                try {
                    formatter = config.formatter().getConstructor().newInstance();

                    if (format != null) {
                        try {
                            formatter.getClass().getMethod("setFormat", String.class);
                        } catch (final Exception e) {
                            logger.warning("Logger format is defined, but Formatter does not have a setFormat(String) method!");
                        }
                    }
                } catch (final Exception e) {
                    logger.log(Level.WARNING, "Configured formatter '" + config.formatter() + "' could not be instantiated via default constructor!", e);
                }
            } else {
                formatter = new DefaultFormatter(format);
            }

            final Formatter formatterToUse = formatter;
            Stream.of(logger.getHandlers()).forEach(h -> h.setFormatter(formatterToUse));

            if (config.filter() != null) {
                try {
                    logger.setFilter(config.filter().getConstructor().newInstance());
                } catch (final Exception e) {
                    logger.log(Level.WARNING, "Configured filter '" + config.filter() + "' could not be instantiated via default constructor!", e);
                }
            }

            logger.setUseParentHandlers(config.useParentHandlers());

            // set resource-bundle if set
            if (!config.resource().baseName().isBlank()) {
                final String localeStr = config.resource().locale();
                Locale locale = null;

                if (localeStr != null && !localeStr.isBlank()) {
                    final String[] split = localeStr.split("-");
                    if (split.length > 0) {
                        final String country = split[0];
                        String language = null;

                        if (split.length > 1) {
                            language = split[1];
                        }

                        locale = new Locale(language, country);
                    } else {
                        locale = Locale.getDefault();
                    }
                } else {
                    locale = Locale.getDefault();
                }

                logger.setResourceBundle(ResourceBundle.getBundle(config.resource().baseName(), locale));
            }

            LogManager.getLogManager().addLogger(logger);

            return logger;
        } else {
            return produceLogger(injectionPoint);
        }
    }

}
