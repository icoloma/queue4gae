package org.queue4gae.queue.mock;

import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.codehaus.jackson.map.ObjectMapper;
import org.queue4gae.queue.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public abstract class AbstractQueueServiceImpl implements QueueService {

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
    private List<Task> delayedTasks = Lists.newArrayList();

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
     * Serializes, deserializes and executes the task
     */
    public void run(Task task) {
        try {
            // inject before serializing, to check that all fields are serializable as JSON
            injectionService.injectMembers(task);
            String s = objectMapper.writeValueAsString(task);
            log.info("Executing " + s);

            // inject after deserializing, for proper execution
            InjectedTask deserialized = objectMapper.readValue(s, InjectedTask.class);
            injectionService.injectMembers(deserialized);
            ((AbstractTask)deserialized).run(this);
            incCompletedTaskCount(task.getQueueName());

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
        if (delayedTasks.size() > 0) {
            log.info("Running " + delayedTasks.size() + " delayed tasks...");
            for (Task task : delayedTasks) {
                run(task);
            }
        }
    }
}
