package com.airepublic.tobi.testsuite.faulttolerance;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

public class FallbackTestClass {
    public int methodCounter = 0;
    public static int handlerCounter = 0;


    @Fallback(fallbackMethod = "theFallbackMethod")
    public void fallbackFallbackMethod() {
        throw new RuntimeException();
    }


    public void theFallbackMethod() {
        methodCounter++;
    }


    @Fallback(TestFallbackHandler.class)
    public void fallbackFallbackHandler() {
        throw new RuntimeException();
    }

    public static class TestFallbackHandler implements FallbackHandler<Void> {

        @Override
        public Void handle(final ExecutionContext context) {
            handlerCounter++;
            return null;
        }
    }
}
