package com.airepublic.microprofile.faulttolerance;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.faulttolerance.Fallback;

public class FallbackBindingAnnotator implements Extension {

    void addFallbackBinding(@Observes final BeforeBeanDiscovery bbd, final BeanManager bm) {
        bbd.addInterceptorBinding(new AnnotatedTypeDecorator<>(bm.createAnnotatedType(Fallback.class), FallbackBinding.class, new AnnotationLiteral<FallbackBinding>() {
            private static final long serialVersionUID = 1L;
        }));
    }
}