package org.queue4gae.queue.mock;

import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.codehaus.jackson.map.ObjectMapper;
import org.queue4gae.queue.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Intended for testing your Task classes.
 * Invoking {@link #post(org.queue4gae.queue.Task)} will execute Task.run() in a separate Thread.
 */
@Singleton
public class MockAsyncQueueService extends AbstractQueueServiceImpl {

    private ExecutorService executorService;

    private BlockingDeque<Task> queue = new LinkedBlockingDeque<Task>();

    private final LocalServiceTestHelper helper;

    private boolean running = true;

    public MockAsyncQueueService(LocalServiceTestHelper helper) {
        this(10, helper);
    }

    public MockAsyncQueueService(int numThreads, LocalServiceTestHelper helper) {
        this.helper = helper;
        this.executorService = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            executorService.execute(new Consumer());
        }
    }

    /**
     * Stop all consumer threads
     */
    public void stop() {
        this.executorService.shutdown();
        //this.executorService.shutdownNow();
    }

    /**
     * Execute the task immediately unless delaySeconds is != null.
     * Recursive invocation of this method (a task pushing another task into the queue) will store the task for
     * serial execution. This method will not return until the last task has finished.
     * @param task
     */
    @Override
    public void post(Task task) {
        try {
            incQueuedTaskCount(task.getQueueName());
            if (task.getDelaySeconds() > 0) {
                pushDelayedTask(task);
            }
            if (task.getTaskName() != null) {
                addTombstone(task.getTaskName());
            }
            queue.putFirst(task);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait until the queue is empty.
     * @throws TimeoutException if the queue is not empty after waiting timeoutInMillis
     */
    public void waitUntilEmpty(int timeoutInMillis) throws TimeoutException {
        Stopwatch watch = new Stopwatch().start();
        do {
            if (getCompletedTaskCount() == getQueuedTaskCount()) {
                return;
            }
        } while (watch.elapsed(TimeUnit.MILLISECONDS) < timeoutInMillis);
        throw new TimeoutException("Timeout waiting for " + (getQueuedTaskCount() - getCompletedTaskCount()) + " queue tasks to complete.");
    }

    /**
     * Consume tasks until the executor is stopped
     */
    private class Consumer implements Runnable {

        @Override
        public void run() {
            helper.setUp();
            try {
                while (true) {
                    Task task = queue.take();
                    int attempts = 0;
                    boolean failed = true;

                    while (failed) {
                        try {
                            MockAsyncQueueService.this.run(task);
                            failed = false;

                        } catch (Exception e) {
                            if (e instanceof InterruptedException) {
                                throw (InterruptedException) e;
                            }
                            log.error(e.toString(), e);

                            // exponential back-off, max 5 seconds
                            int delay = Math.min(attempts++ * 2000, 5000);
                            log.info("Retrying in " + (delay / 1000) + "s");
                            Thread.sleep(delay);
                        }
                    }
                }
            } catch (InterruptedException e) {
                return;
            } finally {
                try {
                    helper.tearDown();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        }
    }

}