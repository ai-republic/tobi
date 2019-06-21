package com.airepublic.microprofile.testsuite.faulttolerance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

public class BulkheadTestClass {
    int counter = 0;
    List<Long> callTimes = new ArrayList<>();


    @Bulkhead(value = 2)
    public String bulkheadWithoutAsynchronous() {
        counter++;
        callTimes.add(System.currentTimeMillis());

        try {
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
        }

        return "Test";
    }


    @Bulkhead(value = 2, waitingTaskQueue = 10)
    @Asynchronous
    public Future<String> bulkheadWithAsynchronous() {
        counter++;
        callTimes.add(System.currentTimeMillis());

        final CompletableFuture<String> future = new CompletableFuture<>();
        future.completeAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
            }

            return "Test";
        });

        return future;
    }

}
