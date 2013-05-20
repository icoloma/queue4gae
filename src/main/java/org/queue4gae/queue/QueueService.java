package org.queue4gae.queue;

/**
 * Makes the request to TaskQueueService
 */
public interface QueueService {

    /**
     * Post the task in a queue for a deferred execution
     */
    public void post(Task task);

    /**
     * Execute the task in the current Thread.
     */
    void run(Task task);

}
