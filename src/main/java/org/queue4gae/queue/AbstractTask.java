package org.queue4gae.queue;

import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * Superclass of all Task implementations.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class AbstractTask implements Task {

    /**
     * Run this task in the current thread. This method should be called internally by {@link QueueService}.
     * @param queueService the queue service that is invoking this task
     */
    abstract void run(QueueService queueService);

}
