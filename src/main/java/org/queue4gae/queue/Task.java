package org.queue4gae.queue;

import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * The main task interface. Implementations of this interface are required to be idempotent, since
 * the same task could be executed more than once.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface Task {

    /** if not null, the delay in seconds to wait before executing the task */
    Long getDelaySeconds();

    /**
     * @return the queue name to use for this task
     */
    String getQueueName();
}
