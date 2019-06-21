package com.airepublic.microprofile.testsuite.faulttolerance;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

@Asynchronous
public class AsynchronousTestClass {
    int counter = 0;


    @Asynchronous
    public CompletableFuture<String> asynchronous() {
        counter++;
        final CompletableFuture<String> result = CompletableFuture.completedFuture("Test");
        return result;
    }


    @Asynchronous
    public Future<String> asynchronousWithExceptionOnFuture() {
        counter++;
        return CompletableFuture.failedFuture(new IOException());
    }


    @Asynchronous
    public CompletionStage<String> asynchronousWithExceptionOnCompletionStage() {
        counter++;
        return CompletableFuture.failedStage(new IOException());
    }


    public CompletableFuture<String> asynchronousOnClass() {
        counter++;
        final CompletableFuture<String> result = CompletableFuture.completedFuture("Test");
        return result;
    }

}
