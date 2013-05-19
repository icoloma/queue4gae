package org.queue4gae.queue;

import org.queue4gae.task.Task;

/**
 * Makes the request to TaskQueueService
 */
public interface QueueService {

    public void post(Task task);

}
