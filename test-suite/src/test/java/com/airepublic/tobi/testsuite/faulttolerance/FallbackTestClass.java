package com.airepublic.tobi.testsuite.faulttolerance;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

/**
 * Test class to test the use of the {@link Fallback} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
public class FallbackTestClass {
    public int methodCounter = 0;
    public static int handlerCounter = 0;


    /**
     * Method with simple method forwarding {@link Fallback} annotation.
     */
    @Fallback(fallbackMethod = "theFallbackMethod")
    public void fallbackFallbackMethod() {
        throw new RuntimeException();
    }


    /**
     * The fallback forwarding method.
     */
    public void theFallbackMethod() {
        methodCounter++;
    }


    /**
     * Method with {@link FallbackHandler} {@link Fallback} annotation.
     */
    @Fallback(TestFallbackHandler.class)
    public void fallbackFallbackHandler() {
        throw new RuntimeException();
    }

    /**
     * The {@link FallbackHandler} test class.
     * 
     * @author Torsten Oltmanns
     *
     */
    public static class TestFallbackHandler implements FallbackHandler<Void> {

        @Override
        public Void handle(final ExecutionContext context) {
            handlerCounter++;
            return null;
        }
    }
}
