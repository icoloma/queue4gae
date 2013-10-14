package org.queue4gae.queue;

import com.google.appengine.api.datastore.Cursor;
import com.google.common.base.Stopwatch;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.concurrent.TimeUnit;

/**
 * A cursor-based task for batch processing.
 * This task may continue a previous execution if a cursor field is provided.
 */
public abstract class CursorTask extends InjectedTask {

    /** Queue requests are limited to 10 minutes */
    public static final long QUEUE_TIMEOUT = 10 * 60 * 1000L;

    /** Datastore queries will timeout after 30 seconds */
    public static final long QUERY_TIMEOUT = 30 * 1000;

    /** Cursor to continue a previous task execution (can be null) */
    @JsonProperty
    private Cursor cursor;

    /** time consumed by the current {@link #run} execution */
    @JsonIgnore
    private Stopwatch queueWatch;

    /** time consumed by the current {@link #runQuery} execution */
    @JsonIgnore
    private Stopwatch queryWatch;

    protected CursorTask() {
    }

    protected CursorTask(String queueName) {
        super(queueName);
    }

    public <T extends InjectedTask> T withCursor(Cursor cursor) {
        this.cursor = cursor;
        return (T) this;
    }

    /**
     * Executes a batch. This method will execute for 10 minutes invoking repeatedly {@link #runQuery} until
     * it returns null or the time is out
     */
    @Override
    public void run(QueueService queueService) {
        startQueueWatch();
        do {
            startQueryWatch();
            cursor = runQuery(cursor);
        } while (cursor != null && !queueTimeOut());

        // if there is still work to do, re-enqueue this task with the new cursor value
        if (cursor != null) {
            queueService.post(this);
        }
    }

    private void startQueryWatch() {
        queryWatch = new Stopwatch().start();
    }

    private void startQueueWatch() {
        queueWatch = new Stopwatch().start();
    }

    /**
     * @return true if the current execution of {@link #run} is close to the 10-minute limit
     * and should exit.
     */
    private boolean queueTimeOut() {
        // the definition of "close to the 10-minute limit" here is 1 minute.
        // This should be enough to execute runQuery again and then re-enqueue any pending work
        return queueWatch.elapsed(TimeUnit.MILLISECONDS) > QUEUE_TIMEOUT - 1 * 60 * 1000L;
    }

    /**
     * @return true if the current execution of {@link #runQuery} is close to the 30-second limit
     * and should exit.
     */
    protected boolean queryTimeOut() {
        // the definition of "close to the 30 second limit" here is 5 seconds.
        // Feel free to override this method if this is not your case
        return queryWatch.elapsed(TimeUnit.MILLISECONDS) > QUERY_TIMEOUT - 5000;
    }

    /**
     * Process results below the 30-second limit. This method should invoke {@link #queryTimeOut}
     * periodically to check if it should exit.
     * This method receives a Cursor to start processing rows, and may return another Cursor
     * if there are still rows to process. If the returned value is not null, the method will be invoked again
     * with the new cursor value if we are still below the 10-minute limit, or it may be re-queued to process later.
     * @param startCursor the cursor to start processing from (may be null)
     * @return a cursor if there are still results to process, null otherwise.
     */
    protected abstract Cursor runQuery(Cursor startCursor);

}
