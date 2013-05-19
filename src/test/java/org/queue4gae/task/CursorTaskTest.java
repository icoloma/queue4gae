package org.queue4gae.task;

import com.google.appengine.api.datastore.*;
import org.junit.Before;
import org.junit.Test;
import org.queue4gae.mock.MockInjectionService;
import org.queue4gae.mock.MockQueueService;

import static org.junit.Assert.assertTrue;

public class CursorTaskTest extends AbstractTest {

    private MockQueueService queueService;

    private MockInjectionService injectionService;

    @Before
    public void setupServices() {
        queueService = new MockQueueService();
        injectionService = new MockInjectionService();
    }

    @Test
    public void testRunQuery() {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        for (int i = 0; i < 2; i++) {
            Entity e = new Entity(KeyFactory.createKey("foo", 1));
            e.setProperty("processed", false);
            ds.put(e);
        }

        MyTask task = new MyTask()
                .withQueueName("foobar");
        task
    }

    // just process one record and return
    private class MyTask extends CursorTask {

        @Override
        protected Cursor runQuery(Cursor startCursor) {
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            Query q = new Query("foo");
            QueryResultIterator<Entity> iterator = ds.prepare(q).asQueryResultIterator(
                    FetchOptions.Builder.withStartCursor(startCursor)
            );
            Entity e = iterator.next();
            e.setProperty("processed", true);
            ds.put(e);
            return iterator.getCursor();
        }
    }

}
