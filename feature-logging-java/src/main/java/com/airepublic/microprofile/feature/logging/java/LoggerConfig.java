package com.airepublic.microprofile.feature.logging.java;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * An annotation which can be provided to an injection point of a {@link Logger} to configure the
 * {@link Logger} instance. <br/>
 * Example:<br/>
 * <br/>
 * <code>&#64;Inject<br/>
 * &#64;LoggerConfig(level=LogLevel.INFO, handler=java.util.logging.ConsoleHandler.class, filter=MyFilter.class, resource=@Resource(baseName="myresourcebundlename", locale="de-DE"))<br/>
 * private Logger logger;
 * </code>
 * 
 * @author Torsten Oltmanns
 *
 */
@Qualifier
@Retention(RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface LoggerConfig {
    public static @interface Resource {

        public String baseName() default "";


        @Nonbinding
        public String locale() default "";

    }

    public static class DefaultFilter implements Filter {
        @Override
        public boolean isLoggable(final LogRecord record) {
            return true;
        }
    }


    @Nonbinding
    String name() default "";


    @Nonbinding
    LogLevel level() default LogLevel.ALL;


    @Nonbinding
    Class<? extends Formatter> formatter() default DefaultFormatter.class;


    @Nonbinding
    String format() default "[%1$tF %1$tT] [%4$-7s] %2$s: %5$s%6$s%n";


    @Nonbinding
    Class<? extends Handler> handler() default ConsoleHandler.class;


    @Nonbinding
    Class<? extends Filter> filter() default DefaultFilter.class;


    @Nonbinding
    Resource resource() default @Resource();


    @Nonbinding
    boolean useParentHandlers() default false;
}
