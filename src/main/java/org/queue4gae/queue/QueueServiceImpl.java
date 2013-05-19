package org.queue4gae.queue;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import org.codehaus.jackson.map.ObjectMapper;
import org.queue4gae.task.InjectedTask;
import org.queue4gae.task.Task;

import javax.inject.Inject;
import java.io.IOException;

public class QueueServiceImpl implements QueueService {

    /** the configured ObjectMapper. Must be capable of deserializing AppEngine classes like Key and Cursor */
    @Inject
    private ObjectMapper objectMapper;

    /** the URL that will handle our queue requests */
    @Inject
    private String taskUrl;

    @Override
    public void post(Task task) {
        try {
            TaskOptions options = TaskOptions.Builder.withDefaults()
                    .method(TaskOptions.Method.POST)
                    .url("/_/task")
                    .payload(objectMapper.writeValueAsString(task).getBytes("utf-8"), "application/json");
            Long delaySeconds = task.getDelaySeconds();
            if (delaySeconds != null) {
                options = options.countdownMillis(delaySeconds * 1000L);
            }
            QueueFactory.getQueue(task.getQueueName()).add(options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
