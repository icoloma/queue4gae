package org.queue4gae.queue;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

public class QueueServiceImpl implements QueueService {

    /** the configured ObjectMapper. Must be capable of deserializing AppEngine classes like Key and Cursor */
    private ObjectMapper objectMapper;

    /** the Injection manager will be used to inject fields into task objects */
    private InjectionService injectionService;

    /** the URL that will handle our queue requests */
    private String taskUrl;

    @Override
    public void post(Task task) {
        try {
            String queueName = task.getQueueName();
            Preconditions.checkArgument(queueName != null, "task.getQueueName() cannot be null");
            TaskOptions options = TaskOptions.Builder.withDefaults()
                    .method(TaskOptions.Method.POST)
                    .url(taskUrl)
                    .payload(objectMapper.writeValueAsString(task).getBytes("utf-8"), "application/json");
            if (task.getTaskName() != null) {
                options = options.taskName(task.getTaskName());
            }
            if (task.getTag() != null) {
                options = options.tag(task.getTag());
            }
            if (task.getDelaySeconds() != 0) {
                options = options.countdownMillis(task.getDelaySeconds() * 1000L);
            }
            QueueFactory.getQueue(queueName).add(options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(Task task) {
        injectionService.injectMembers(task);
        ((AbstractTask)task).run(this);
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Inject
    public void setInjectionService(InjectionService injectionService) {
        this.injectionService = injectionService;
    }

    @Inject
    public void setTaskUrl(@Named(TASK_URL) String taskUrl) {
        this.taskUrl = taskUrl;
    }

}
