package org.queue4gae.queue.mock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.queue4gae.queue.*;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Intended for testing your Task classes.
 * Invoking {@link #post(org.queue4gae.queue.Task)} will execute Task.run() synchronously and return.
 */
@Singleton
public class MockQueueService extends AbstractQueueServiceImpl {

    /** our queue of tasks */
    protected Queue<Task> tasks = new ConcurrentLinkedQueue<Task>();

    /**
     * Add a task to the internal queue representation
     */
    protected void pushTask(Task task) {
        tasks.add(task);
        if (task.getTaskName() != null) {
            addTombstone(task.getTaskName());
        }
    }

    /**
     * Execute the task immediately unless delaySeconds is != null.
     * Recursive invocation of this method (a task pushing another task into the queue) will store the task for
     * serial execution. This method will not return until the last task has finished.
     * @param task
     */
    @Override
    public void post(Task task) {
        incQueuedTaskCount(task.getQueueName());
        if (task.getDelaySeconds() == 0) {
            pushTask(task);
            if (tasks.size() == 1) {
                // we are the first level of post(), not a recursive task-starts-task scenario
                while (!tasks.isEmpty()) {
                    for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
                        Task t = it.next();
                        run(t);
                        it.remove();
                    }
                }
            }
        } else {
            pushDelayedTask(task);
        }
    }

}
