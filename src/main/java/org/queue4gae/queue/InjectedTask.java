package org.queue4gae.queue;

/**
 * A task that will be deserialized from JSON and dependency injected before execution.
 * Any field annotated with javax.inject.Inject will be injected before invoking doRun().
 * @author icoloma
 */
@SuppressWarnings("unchecked")
public abstract class InjectedTask extends AbstractTask {

	private String queueName;

    private Long delaySeconds;

    protected InjectedTask() {
    }

    protected InjectedTask(String queueName) {
		this.queueName = queueName;
	}
/*
    public <T extends InjectedTask> T withQueueName(String queueName) {
        this.queueName = queueName;
        return (T) this;
    }
*/
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

}
