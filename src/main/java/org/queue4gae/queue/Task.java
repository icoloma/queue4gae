package org.queue4gae.queue;

import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * The main task interface. Implementations of this interface are required to be idempotent, since
 * the same task could be executed more than once.
 *
 * This use of generics to mix fluent interfaces with inheritance is explained here:
 * http://stackoverflow.com/questions/7354740/is-there-a-way-to-refer-to-the-current-type-with-a-type-variable/7355094
 *
 * @param <T> the type of the child class
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface Task<T> {

    /**
     * @return the queue name to use for this task.
     */
    String getQueueName();

    /**
     * Set the queue name to use. It can be specified either here or in the constructor.
     */
    T withQueueName(String queueName);

    /**
     * Set the task name to use. If no task name is defined, AppEngine will assign an ID automatically.
     */
    T withTaskName(String taskName);

    /**
     * @return the task name, if any.
     */
    String getTaskName();

    /**
     * Set the tag String. Only valid for pull queues.
     */
    T withTag(String tag);

    /**
     * @return the tag, if any.
     */
    String getTag();

    /**
     * Set a number of seconds to wait before executing this task.
     */
    T withDelaySeconds(Long delaySeconds);

    /**
     * @return number of seconds to wait before executing this task. May be null, in which case it will be executed as soon as possible.
     */
    Long getDelaySeconds();

}
