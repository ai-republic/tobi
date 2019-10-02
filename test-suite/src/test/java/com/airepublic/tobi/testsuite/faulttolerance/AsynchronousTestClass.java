package com.airepublic.tobi.testsuite.faulttolerance;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

/**
 * Test class to test the use of the {@link Asynchronous} annotation.
 * 
 * @author Torsten Oltmanns
 *
 */
@Asynchronous
public class AsynchronousTestClass {
    int counter = 0;


    /**
     * Simple annotated method.
     * 
     * @return the {@link Future}
     */
    @Asynchronous
    public CompletableFuture<String> asynchronous() {
        counter++;
        final CompletableFuture<String> result = CompletableFuture.completedFuture("Test");
        return result;
    }


    /**
     * {@link Future} that failed.
     * 
     * @return the {@link Future}
     */
    @Asynchronous
    public Future<String> asynchronousWithExceptionOnFuture() {
        counter++;
        return CompletableFuture.failedFuture(new IOException());
    }


    /**
     * {@link CompletionStage} that failed.
     * 
     * @return the {@link CompletionStage}
     */
    @Asynchronous
    public CompletionStage<String> asynchronousWithExceptionOnCompletionStage() {
        counter++;
        return CompletableFuture.failedStage(new IOException());
    }


    /**
     * {@link Asynchronous} on class level.
     * 
     * @return the {@link Future}
     */
    public CompletableFuture<String> asynchronousOnClass() {
        counter++;
        final CompletableFuture<String> result = CompletableFuture.completedFuture("Test");
        return result;
    }

}
