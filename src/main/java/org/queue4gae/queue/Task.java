package org.queue4gae.queue;

/**
 * The main task interface. Implementations of this interface are required to be idempotent, since
 * the same task could be executed more than once.
 */
public interface Task {

    /** if not null, the delay in seconds to wait before executing the task */
    Long getDelaySeconds();

    /**
     * @return the queue name to use for this task
     */
    String getQueueName();
}
