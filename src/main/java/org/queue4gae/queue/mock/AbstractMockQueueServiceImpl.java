package org.queue4gae.queue.mock;

import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.codehaus.jackson.map.ObjectMapper;
import org.queue4gae.queue.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

public abstract class AbstractMockQueueServiceImpl <T extends AbstractMockQueueServiceImpl> implements QueueService {

    protected static final String DEFAULT_QUEUE_NAME = "default";

    private InjectionService injectionService;

    /** tombstoned task names */
    private Set<String> tombstones = Sets.newCopyOnWriteArraySet();

    private ObjectMapper objectMapper;

    /** count of queued tasks */
    private Multiset<String> queuedTaskCount = ConcurrentHashMultiset.create();

    /** count of completed tasks */
    private Multiset<String> completedTaskCount = ConcurrentHashMultiset.create();

    /** delayed tasks */
    private Queue<Task> delayedTasks = new PriorityBlockingQueue<Task>(100, new DelayedTaskComparator());

    /** if not null, applies this delay to all queued tasks */
    protected Integer delaySeconds;

    /** the number of times a task will be retried, by default 0 (any exception will fail the test) */
    protected int retries = 0;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Add a task name to the list of tombstones.
     * @throws TaskAlreadyExistsException if the task is already registered
     */
    protected void addTombstone(String taskName) {
        if (!tombstones.add(taskName)) {
            throw new TaskAlreadyExistsException("Task name '" + taskName + "' is already in the queue");
        }
    }

    /**
     * Extension point to set up anything related to the current Task and Thread, like ThreadLocal variables
     * @param task the task about to be executed
     */
    protected void setupTask(Task task) {
    }

    /**
     * Extension point to clean up anything related to the current Task and Thread, like ThreadLocal variables
     * @param task the task that just finished execution
     */
    protected void teardownTask(Task task) {
    }

    /**
     * Serializes, deserializes and executes the task
     */
    public void run(Task task) {
        try {
            // inject before serializing, to check that all fields are serializable as JSON
            injectionService.injectMembers(task);

            String s = objectMapper.writeValueAsString(task);
            log.info("Executing " + s);

            // inject after deserializing, for proper execution
            AbstractTask deserialized = objectMapper.readValue(s, AbstractTask.class);
            injectionService.injectMembers(deserialized);
            setupTask(task);

            try {
                ((AbstractTask)deserialized).run(this);
                incCompletedTaskCount(task.getQueueName());
            } finally {
                teardownTask(task);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void incQueuedTaskCount(String queueName) {
        queuedTaskCount.add(queueNameOrDefault(queueName));
    }

    private String queueNameOrDefault(String queueName) {
        return queueName == null? DEFAULT_QUEUE_NAME : queueName;
    }

    /**
     * @return the number of task instances queued, including all queue names
     */
    public int getQueuedTaskCount() {
        return queuedTaskCount.size();
    }

    /**
     * @return the number of task instances queued for the provided queue name
     */
    public int getQueuedTaskCount(String queueName) {
        return queuedTaskCount.count(queueNameOrDefault(queueName));
    }

    private void incCompletedTaskCount(String queueName) {
        completedTaskCount.add(queueNameOrDefault(queueName));
    }

    /**
     * @return the number of tasks completed, including all queue names
     */
    public int getCompletedTaskCount() {
        return completedTaskCount.size();
    }

    /**
     * @return the number of tasks completed for the provided queue name
     */
    public int getCompletedTaskCount(String queueName) {
        return completedTaskCount.count(queueNameOrDefault(queueName));
    }

    /**
     * @return the number of delayed tasks still pending execution
     */
    public int getDelayedTaskCount() {
        return delayedTasks.size();
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Inject
    public void setInjectionService(InjectionService injectionService) {
        this.injectionService = injectionService;
    }

    protected void pushDelayedTask(Task task) {
        delayedTasks.add(task);
    }

    /**
     * Execute delayed tasks
     */
    public void runDelayedTasks() {
        runDelayedTasks(Task.class);
    }

    /**
     * Execute delayed tasks of the given type
     */
    public void runDelayedTasks(Class<? extends Task> taskClass) {
        log.info("Running delayed tasks...");
        serializeExecutionOfTasks(delayedTasks, taskClass);
    }

    /**
     * Serialize the execution of all tasks in the queue. If one tasks create a new task, it will be executed too.
     * This method will return when the tasks list is empty
     * @param tasks
     */
    public void serializeExecutionOfTasks(Collection<Task> tasks, Class<? extends Task> taskClass) {
        int tasksRun = 1;
        while (tasksRun > 0) {
            tasksRun = 0;
            for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
                int attempts = 0;
                Task t = it.next();
                while (true) {
                    try {
                        if (taskClass.isAssignableFrom(t.getClass())) {
                            tasksRun++;
                            run(t);
                            it.remove();
                        }
                        break;
                    } catch (RuntimeException e) {
                        if (attempts++ >= retries) {
                            throw e;
                        }
                        log.error(e.toString(), e);
                        log.info("Retrying " + attempts + " of " + retries);
                    }
                }
            }
        }
    }

    public T withDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
        return (T) this;
    }

    public T withRetries(int retries) {
        this.retries = retries;
        return (T) this;
    }

}
