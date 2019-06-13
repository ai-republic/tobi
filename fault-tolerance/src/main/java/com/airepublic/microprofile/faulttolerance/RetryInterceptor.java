package com.airepublic.microprofile.faulttolerance;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Retry;

@Retry
@Interceptor
public class RetryInterceptor {
    private int fail = 0;


    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {

        try {
            final Object result = context.proceed();
            return result;
        } catch (final Exception e) {
            fail++;

            throw e;
        }
    }

}
