package org.queue4gae.queue.mock;

import com.google.appengine.api.ThreadManager;
import com.google.common.base.Stopwatch;
import org.queue4gae.queue.Task;

import javax.inject.Singleton;
import java.util.concurrent.*;

/**
 * Intended for testing your Task classes.
 * Invoking {@link #post(org.queue4gae.queue.Task)} will execute Task.run() in a separate Thread. Threads are launched using {@link #start()},
 * and are stopped invoking {@link #stop()}.
<pre>
\@Before
public void setupServices() {
    helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    helper.setUp();

    queue = new MockAsyncQueueService();
    queue.start();
}

 \@After
 public void tearDown() {
    queue.stop();
    helper.tearDown();
 }
 </pre>
 */
@Singleton
public class MockAsyncQueueService extends AbstractMockQueueServiceImpl<MockAsyncQueueService> {

    /** number of consumer threads to span */
    private int numThreads;

    private ExecutorService executorService;

    private BlockingDeque<Task> queue = new LinkedBlockingDeque<Task>();

    public MockAsyncQueueService() {
        this(10);
    }

    public MockAsyncQueueService(int numThreads) {
        this.numThreads = numThreads;
    }

    /**
     * Start the consumer threads
     */
    public void start() {
        this.executorService = Executors.newFixedThreadPool(numThreads, ThreadManager.currentRequestThreadFactory());
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
     * serial execution. This method will return immediately.
     * @param task
     */
    @Override
    public void post(Task task) {
        try {
            if (delaySeconds != null && task.getDelaySeconds() == 0) {
                task.withDelaySeconds(delaySeconds);
            }
            incQueuedTaskCount(task.getQueueName());
            if (task.getTaskName() != null) {
                addTombstone(task.getTaskName());
            }
            if (task.getDelaySeconds() > 0) {
                pushDelayedTask(task);
            } else {
                queue.putFirst(task);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait until all tasks with no delay have been executed.
     * @see #runDelayedTasks()
     * @throws TimeoutException if the queue is not empty after waiting timeoutInMillis.
     */
    public void waitUntilEmpty(int timeoutInMillis) throws TimeoutException {
        Stopwatch watch = Stopwatch.createStarted();
        do {
            if (getCompletedTaskCount() + getDelayedTaskCount() == getQueuedTaskCount()) {
                return;
            }
        } while (watch.elapsed(TimeUnit.MILLISECONDS) < timeoutInMillis);
        throw new TimeoutException("Timeout waiting for " + (getQueuedTaskCount() - getCompletedTaskCount()) + " queue tasks to complete.");
    }

    /**
     * Invokes waitUntilEmpty(1000), then executed all delayed tasks.
     */
    @Override
    public void runDelayedTasks() {
        try {
            waitUntilEmpty(1000);
            super.runDelayedTasks();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Consume tasks until the executor is stopped
     */
    private class Consumer implements Runnable {

        @Override
        public void run() {
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
            }
        }
    }

}