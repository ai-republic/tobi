package com.airepublic.tobi.feature.mp.faulttolerance;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * CDI {@link Extension} to decorate occurrences of the {@link Fallback} annotation with the
 * {@link FallbackBinding} annotation because it is only allowed at method scope.
 * 
 * @author Torsten Oltmanns
 *
 */
public class FallbackAnnotationDecorator implements Extension {

    /**
     * Decorate the occurrence of the {@link Fallback} annotation with the {@link FallbackBinding}
     * anntation.
     * 
     * @param event the {@link BeforeBeanDiscovery} event
     * @param bm the {@link BeanManager}
     */
    void addFallbackBinding(@Observes final BeforeBeanDiscovery event, final BeanManager bm) {
        event.addInterceptorBinding(new AnnotatedTypeDecorator<>(bm.createAnnotatedType(Fallback.class), FallbackBinding.class, new AnnotationLiteral<FallbackBinding>() {
            private static final long serialVersionUID = 1L;
        }));
    }
}