package org.queue4gae.task;

/**
 * The main task interface. Implementations of this interface are required to be idempotent, since
 * the same task could be executed more than once.
 */
public interface Task {

    /**
     * Post the task in a queue for a deferred execution
     */
    public void post();

    /**
     * Execute this task in the current Thread.
     */
    public void run();

    /** if not null, the delay in seconds to wait before executing the task */
    Long getDelaySeconds();

    /**
     * @return the queue name to use for this task
     */
    String getQueueName();
}
