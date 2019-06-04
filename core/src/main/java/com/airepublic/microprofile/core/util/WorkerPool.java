package com.airepublic.microprofile.core.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;

public class WorkerPool<I, R> {
    private final ExecutorService executor;
    private final Semaphore semaphore;
    private final Queue<IOWorker> availableWorkers = new ConcurrentLinkedDeque<>();

    class IOWorker implements Runnable {
        private final WorkerPool<I, R> workerPool;
        private Function<I, R> task;
        private I input;
        private Consumer<R> success;
        private Consumer<Throwable> fail;


        public IOWorker(final WorkerPool<I, R> workerPool) {
            this.workerPool = workerPool;
        }


        void prepare(final Function<I, R> task, final I input) {
            prepare(task, input, null, null);
        }


        void prepare(final Function<I, R> task, final I input, final Consumer<R> success) {
            prepare(task, input, success, null);
        }


        void prepare(final Function<I, R> task, final I input, final Consumer<R> success, final Consumer<Throwable> fail) {
            this.task = task;
            this.input = input;
            this.success = success;
            this.fail = fail;
        }


        @Override
        public void run() {
            try {
                final R result = task.apply(input);

                if (success != null) {
                    success.accept(result);
                }
            } catch (final Throwable t) {
                if (fail != null) {
                    fail.accept(t);
                }
            }

            workerPool.free(this);
        }
    }


    public WorkerPool(final int poolSize) {
        semaphore = new Semaphore(poolSize);

        for (int i = 0; i < poolSize; i++) {
            availableWorkers.add(new IOWorker(this));
        }

        executor = Executors.newWorkStealingPool(poolSize);
    }


    public void assign(final Function<I, R> task, final I input) throws InterruptedException {
        assign(task, input, null, null);
    }


    public void assign(final Function<I, R> task, final I input, final Consumer<R> success) throws InterruptedException {
        assign(task, input, success, null);
    }


    public void assign(final Function<I, R> task, final I input, final Consumer<R> success, final Consumer<Throwable> fail) throws InterruptedException {
        semaphore.acquire();

        final IOWorker worker = availableWorkers.poll();
        worker.prepare(task, input, success, fail);

        executor.execute(worker);
    }


    private void free(final IOWorker worker) {
        availableWorkers.add(worker);
        semaphore.release();
    }

}
