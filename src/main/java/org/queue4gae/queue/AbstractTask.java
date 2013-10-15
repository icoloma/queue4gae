package org.queue4gae.queue;

import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * Superclass of all Task implementations.
 *
 */
public abstract class AbstractTask<T> implements Task {

    /** The queue name to use */
    private String queueName;

    /** The task name. May be null, in which case AppEngine will generate an ID automatically. */
    protected String taskName;

    /** The tag name to be used by pull queues. May be null. */
    protected String tag;

    /** A number of seconds to wait before execution. May be null. */
    private Long delaySeconds;

    /**
     * Run this task in the current thread. This method should be called internally by {@link QueueService}.
     * @param queueService the queue service that is invoking this task
     */
    public abstract void run(QueueService queueService);

    protected AbstractTask() {
    }

    protected AbstractTask(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public T withTag(String tag) {
        this.tag = tag;
        return (T) this;
    }

    @Override
    public T withTaskName(String taskName) {
        this.taskName = taskName;
        return (T) this;
    }

    @Override
    public String getQueueName() {
        return queueName;
    }

    @Override
    public T withDelaySeconds(Long delaySeconds) {
        this.delaySeconds = delaySeconds;
        return (T) this;
    }

    @Override
    public T withQueueName(String queueName) {
        this.queueName = queueName;
        return (T) this;
    }

    @Override
    public Long getDelaySeconds() {
        return delaySeconds;
    }
}
