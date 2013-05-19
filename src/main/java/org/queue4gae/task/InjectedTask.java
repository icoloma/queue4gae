package org.queue4gae.task;

import com.google.appengine.api.datastore.Cursor;
import com.google.common.base.Stopwatch;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.queue4gae.inject.InjectionService;
import org.queue4gae.queue.QueueService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A task that will be deserialized from JSON and dependency injected before execution.
 * Any field annotated with javax.inject.Inject will be injected before invoking doRun().
 * @author icoloma
 */
@SuppressWarnings("unchecked")
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class InjectedTask implements Task {

	private String queueName;

    private Long delaySeconds;

    protected QueueService queueService;

    protected InjectedTask() {
        // this constructor is for Jackson
    }

    protected InjectedTask(String queueName) {
        this();
		this.queueName = queueName;
	}

    @Override
	public void post() {
        queueService.post(this);
	}

    public <T extends InjectedTask> T withQueueName(String queueName) {
        this.queueName = queueName;
        return (T) this;
    }

	public String getQueueName() {
		return queueName;
	}

    public <T extends InjectedTask> T withDelaySeconds(long delaySeconds) {
        this.delaySeconds = delaySeconds;
        return (T) this;
    }

    public Long getDelaySeconds() {
        return delaySeconds;
    }

    @Inject @JsonIgnore
    public void setQueueService(QueueService queueService) {
        this.queueService = queueService;
    }
}
