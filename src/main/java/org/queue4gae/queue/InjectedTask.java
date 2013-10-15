package org.queue4gae.queue;

/**
 * A task that will be deserialized from JSON and dependency injected before execution.
 * This class exists to simplify things so nobody has to understand the Curiously Recurring Template Pattern.
 * Other may find OK to just extend AbstractTask. They just do the same.
 *
 * @see Task
 * @author icoloma
 */
public abstract class InjectedTask extends AbstractTask<InjectedTask> {

    protected InjectedTask() {
    }

    protected InjectedTask(String queueName) {
		super(queueName);
	}

}
