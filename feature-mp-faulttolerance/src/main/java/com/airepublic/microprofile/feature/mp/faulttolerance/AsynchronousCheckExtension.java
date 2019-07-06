package com.airepublic.microprofile.feature.mp.faulttolerance;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

public class AsynchronousCheckExtension implements Extension {
    private Throwable t;


    void checkAsynchronousUse(@Observes @WithAnnotations(Asynchronous.class) final ProcessAnnotatedType<?> event, final BeanManager bm) {
        if (!event.getAnnotatedType().getJavaClass().isAnnotationPresent(Interceptor.class)) {
            if (event.getAnnotatedType().getJavaClass().isAnnotationPresent(Asynchronous.class)) {
                // check all methods for Future or CompletionStage return-type
                if (!Stream.of(event.getAnnotatedType().getJavaClass().getDeclaredMethods()).allMatch(m -> Future.class.isAssignableFrom(m.getReturnType()) || CompletionStage.class.isAssignableFrom(m.getReturnType()))) {
                    t = new FaultToleranceDefinitionException("Class " + event.getAnnotatedType().getJavaClass().getName() + " annotated with Asynchronous contains methods which do not have the return-type Future or CompletionStage");
                }
            } else {
                // check Asynchronous annotated methods for Future or CompletionStage return-type
                if (!Stream.of(event.getAnnotatedType().getJavaClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Asynchronous.class)).allMatch(m -> Future.class.isAssignableFrom(m.getReturnType()) || CompletionStage.class.isAssignableFrom(m.getReturnType()))) {
                    t = new FaultToleranceDefinitionException("Method annotated with Asynchronous does not have the return-type Future or CompletionStage in class " + event.getAnnotatedType().getJavaClass().getName());
                }
            }
        }
    }


    void throwExceptionIfAsynchronousUseFails(@Observes final AfterDeploymentValidation event, final BeanManager bm) {
        if (t != null) {
            event.addDeploymentProblem(t);
        }
    }
}
