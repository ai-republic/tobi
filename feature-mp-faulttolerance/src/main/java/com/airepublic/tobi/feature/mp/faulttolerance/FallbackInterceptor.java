package com.airepublic.tobi.feature.mp.faulttolerance;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Implementation to intercept {@link Fallback} annotations.
 * 
 * See {@link Fallback} annotation for details on the specified mechanism
 * 
 * @author Torsten Oltmanns
 *
 */
@Interceptor
@FallbackBinding
public class FallbackInterceptor implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * Intercepts and processes the {@link Fallback} annotation.
     * 
     * @param context the {@link InvocationContext}
     * @return the method result
     * @throws Exception if an error occurs
     */
    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        final Fallback fallback = context.getMethod().getAnnotation(Fallback.class);
        final Class<? extends FallbackHandler<?>> handler = fallback.value() != null && fallback.value() == Fallback.DEFAULT.class ? null : fallback.value();
        final String methodName = fallback.fallbackMethod() != null && fallback.fallbackMethod().isBlank() ? null : fallback.fallbackMethod();

        if (handler != null && methodName != null) {
            throw new FaultToleranceDefinitionException("Only either Fallback value or fallbackMethod is allowed to be set!");
        }

        try {
            return context.proceed();
        } catch (final Throwable t) {
            if (methodName != null) {
                final Class<?>[] parameterTypes = Stream.of(context.getParameters()).map(o -> o.getClass()).toArray(size -> new Class<?>[size]);
                final Method method = context.getTarget().getClass().getMethod(methodName, parameterTypes);
                return method.invoke(context.getTarget(), context.getParameters());
            } else {
                final ExecutionContext executionContext = new ExecutionContext() {

                    @Override
                    public Object[] getParameters() {
                        return context.getParameters();
                    }


                    @Override
                    public Method getMethod() {
                        return context.getMethod();
                    }


                    @Override
                    public Throwable getFailure() {
                        return t;
                    }
                };

                return handler.getConstructor().newInstance().handle(executionContext);
            }
        }
    }

}
