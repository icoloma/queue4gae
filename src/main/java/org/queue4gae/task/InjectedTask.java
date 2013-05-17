package org.queue4gae.task;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.taskqueue.TaskQueuePb;
import com.google.common.base.Stopwatch;
import com.koliseo.config.GuiceConfigListener;
import com.koliseo.service.QueueService;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.simpleds.EntityManager;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Adds {@link Inject} support to Task objects.
 * Jackson will serialize and deserialize this object
 * @author icoloma
 */
@SuppressWarnings("unchecked")
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class InjectedTask implements Runnable {
	
    /** las peticiones de queues en GAE tienen un límite de 10 minutos. Aplicamos un margen de 1 minuto */
    private static final long QUEUE_TIMEOUT = 9 * 60 * 1000L;

    /** las consultas en GAE tienen un timeout de 30 segundos. Le aplicamos un margen de 5 */
    protected static final long queryTimeout= 25 * 1000;

	/** Cursor para continuar con una operación (puede ser null) */
	protected Cursor cursor;

	/** queue name a usar */
	private String queueName;

    /** si no es null, el delay en segundos que va a tardar en ejecutarse la tarea */
    private Long delaySeconds;
	
    protected InjectedTask() {
        // only for Jackson deserialization
    }

    protected InjectedTask(String queueName) {
        this();
		this.queueName = queueName;
	}
    
    protected static String formatDate(Date date) {
    	return new SimpleDateFormat("yyyyMMdd'T'HH:mm").format(date);
    }

    @Override
    public final void run() {
        Stopwatch watch = new Stopwatch().start();
        do {
            cursor = processResults();
        } while (cursor != null && watch.elapsedMillis() < QUEUE_TIMEOUT);
        if (cursor != null) {
            this.post();
        }
    }
    
    /**
     * Process results below the QUERY_LIMIT limit. If there are still results pending, return anything that is not null
     */
    protected abstract Cursor processResults();
    
	public <T extends InjectedTask> T withCursor(Cursor cursor) {
    	this.cursor = cursor;
    	return (T) this;
    }

	/**
	 * Encola una petición POST 
	 */
	public final <T extends InjectedTask> T  post() {
        TaskQ.post(this);
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
}
