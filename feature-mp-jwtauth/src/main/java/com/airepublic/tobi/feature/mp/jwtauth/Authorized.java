package com.airepublic.tobi.feature.mp.jwtauth;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * An annotation to mark methods to require a valid JWT token with groups in matching either the
 * allow or deny parameter.
 * 
 * @author Torsten Oltmanns
 *
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@InterceptorBinding
public @interface Authorized {
    /**
     * Allowed groups to execute the method.
     * 
     * @return the allowed groups
     */
    @Nonbinding
    String[] allow() default {};


    /**
     * Denied groups to execute the method.
     * 
     * @return the denied groups
     */
    @Nonbinding
    String[] deny() default {};
}
