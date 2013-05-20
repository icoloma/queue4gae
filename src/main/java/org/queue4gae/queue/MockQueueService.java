package org.queue4gae.queue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Intended to test your Task classes.
 * When posting, this implementation will just invoke Task.run() synchronously and return. It keeps track of the number
 * of times that it has been invoked for each queue name.
 */
@Singleton
public class MockQueueService implements QueueService {

    private ObjectMapper objectMapper;

    /** count the number of posted tasks */
    private Map<String, Integer> taskCount = Maps.newHashMap();

    /** delayed tasks */
    private List<Task> delayedTasks = Lists.newArrayList();

    private static final Logger log = LoggerFactory.getLogger(MockQueueService.class);

    /**
     * Execute the task immediately unless delaySeconds is != null.
     * @param task
     */
    @Override
    public void post(Task task) {
        incTaskCount(task.getQueueName());
        if (task.getDelaySeconds() == null) {
            run(task);
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
            String s = objectMapper.writeValueAsString(task);
            log.info("Executing " + s);
            InjectedTask deserialized = objectMapper.readValue(s, InjectedTask.class);
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

}
