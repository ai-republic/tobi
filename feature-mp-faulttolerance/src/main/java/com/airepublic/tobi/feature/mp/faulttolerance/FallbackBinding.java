package com.airepublic.tobi.feature.mp.faulttolerance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * Annotation used to decorate {@link Fallback} annotation occurrences due to it only being method
 * scoped.
 * 
 * @author Torsten Oltmanns
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@InterceptorBinding
public @interface FallbackBinding {
}