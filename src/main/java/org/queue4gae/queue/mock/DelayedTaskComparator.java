package org.queue4gae.queue.mock;

import org.queue4gae.queue.Task;

import java.util.Comparator;

/**
 * Used to sort tasks acording to the specified delay
 */
public class DelayedTaskComparator implements Comparator<Task> {

    @Override
    public int compare(Task t1, Task t2) {
        return t1.getDelaySeconds() - t2.getDelaySeconds();
    }

}
