package org.queue4gae.queue;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Intended to test your Task classes.
 * When posting, this implementation will just invoke Task.run() synchronously and return. It keeps track of the number
 * of times that it has been invoked for each queue name.
 */
@Singleton
public class MockQueueService implements QueueService {

    private InjectionService injectionService;

    private ObjectMapper objectMapper;

    /** count the number of posted tasks */
    private Map<String, Integer> taskCount = Maps.newHashMap();

    /** immediate tasks to be executed now */
    private Queue<Task> tasks = new ConcurrentLinkedQueue<Task>();

    /** delayed tasks */
    private List<Task> delayedTasks = Lists.newArrayList();

    /** used task names */
    private Set<String> tombstones = Sets.newHashSet();

    private static final Logger log = LoggerFactory.getLogger(MockQueueService.class);

    /**
     * Execute the task immediately unless delaySeconds is != null.
     * Recursive invocation of this method will store the task for later execution after the current one finishes.
     * If a task posts another task instance, the current execution will end before starting the child task.
     * @param task
     */
    @Override
    public void post(Task task) {
        incTaskCount(task.getQueueName());
        if (task.getDelaySeconds() == 0) {
            tasks.add(task);
            if (task.getTaskName() != null) {
                Preconditions.checkArgument(tombstones.add(task.getTaskName()), "Taskname %s already used", task.getTaskName());
            }
            if (tasks.size() == 1) {
                // we are the first level of post(), not a recursive task-starts-task scenario
                while (!tasks.isEmpty()) {
                    for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
                        run(it.next());
                        it.remove();
                    }
                }
            }
        } else {
            delayedTasks.add(task);
        }
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void incTaskCount(String queueName) {
        Integer i = taskCount.get(queueName);
        taskCount.put(queueName, i == null? 1 : i + 1);
    }

    /**
     * @return the total number of task instances queued, including all queues
     */
    public int getTaskCount() {
        int sum = 0;
        for (Integer i : taskCount.values())
            sum += i;
        return sum;
    }

    /**
     * @return the total number of task instances queued for the provided queue name
     */
    public int getTaskCount(String queueName) {
        Integer i = taskCount.get(queueName);
        return i == null? 0 : i.intValue();
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Inject
    public void setInjectionService(InjectionService injectionService) {
        this.injectionService = injectionService;
    }
}
