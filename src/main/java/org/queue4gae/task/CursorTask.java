package org.queue4gae.task;

import com.google.appengine.api.datastore.Cursor;
import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

/**
 * A cursor-based task for batch processing.
 * This task may continue a previous execution if a cursor field is provided.
 */
public abstract class CursorTask extends InjectedTask {

    /** Queue requests are limited to 10 minutes. We are giving 1 minute of margin of error */
    private static final long QUEUE_TIMEOUT = 9 * 60 * 1000L;

    /** Datastore queries will timeout after 30 seconds. Le aplicamos un margen de 5 */
    protected static final long QUERY_TIMEOUT = 30 * 1000;

    /** Cursor to continue a previous task execution (can be null) */
    private Cursor cursor;

    /** used to measure the time consumed by the current runQuery execution */
    private Stopwatch queryWatch;

    public <T extends InjectedTask> T withCursor(Cursor cursor) {
        this.cursor = cursor;
        return (T) this;
    }

    /**
     * Executes a batch. This method will execute for 10 minutes invoking runIn30
     */
    @Override
    public void run() {
        Stopwatch queueWatch = new Stopwatch().start();
        do {
            queryWatch = new Stopwatch().start();
            cursor = runQuery(cursor);
        } while (cursor != null && queueWatch.elapsed(TimeUnit.MILLISECONDS) < QUEUE_TIMEOUT);
        if (cursor != null) {
            this.post();
        }
    }

    /**
     * @return true if the current execution of runQuery is close to the 30 second limit
     * and should exit.
     */
    protected boolean hasTimedOut() {
        // the definition of "close to the 30 second limit" here is 5 seconds.
        // Feel free to override this method if this is not your case
        return queryWatch.elapsed(TimeUnit.MILLISECONDS) < (QUERY_TIMEOUT - 5000);
    }

    /**
     * Process results below the 30-second limit. This method should invoke {@link #hasTimedOut}
     * periodically to check if it should exit.
     * This method receives a Cursor to start processing rows, and may return another Cursor
     * if there is still some work pending. If there is still work pending, it may be executed again
     * by this same task instance (in case we are still below the 10-minute limit) or re-enqueued for
     * processing later.
     * @param startCursor the cursor to start processing from (could be null)
     * @return a cursor if there are still results pending, null otherwise.
     */
    protected abstract Cursor runQuery(Cursor startCursor);

}
