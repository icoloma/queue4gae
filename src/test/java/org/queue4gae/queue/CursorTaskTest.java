package org.queue4gae.queue;

import com.google.appengine.api.datastore.*;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.j4gae.ObjectMapperSetup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CursorTaskTest extends AbstractTest {

    private static final int ENTITY_COUNT= 3;
    public static final String KIND = "foo";

    private MockQueueService queueService;

    private DatastoreService ds;

    @Before
    public void setupServices() {
        ds = DatastoreServiceFactory.getDatastoreService();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        ObjectMapperSetup.addMixins(objectMapper, Cursor.class);

        queueService = new MockQueueService();
        queueService.setObjectMapper(objectMapper);
    }

    /**
     * Iterates over all rows in a single post execution: since we are below the ten-minute limit, runQuery() is
     * invoked three times
     */
    @Test
    public void testRunSinglePost() throws Exception {
        initData();
        OneRowTask task = new OneRowTask(false);
        queueService.post(task);
        assertEquals(1, queueService.getTaskCount());
        checkData();
    }

    /**
     * Iterates over all rows using multiple post execution: since we have exceeded the ten-minute limit, the task is re-posted three times
     */
    @Test
    public void testRunMultiplePost() throws Exception {
        initData();
        OneRowTask task = new OneRowTask(true);
        queueService.post(task);
        assertEquals(ENTITY_COUNT, queueService.getTaskCount());
        checkData();
    }

    @Test(expected = RuntimeException.class)
    public void testUnserializableTask() {
        queueService.post(new UnserializableTask());
    }

    private void checkData() throws EntityNotFoundException {
        for (int i = 0; i < ENTITY_COUNT; i++) {
            Entity e = ds.get(KeyFactory.createKey(KIND, i + 1));
            assertTrue((Boolean) e.getProperty("processed"));
        }
    }

    private void initData() {
        for (int i = 0; i < ENTITY_COUNT; i++) {
            Entity e = new Entity(KeyFactory.createKey(KIND, i + 1));
            e.setProperty("processed", false);
            ds.put(e);
        }
    }

    /**
     * Unserializable task
     */
    public static class UnserializableTask extends InjectedTask {

        /** this attribute is here only to make it impossible to serialize to JSON */
        private DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

        @Override
        void run(QueueService queueService) {
            Assert.fail("This class should not deserialize properly");
        }
    }

    /**
     * Just process one row and return.
     * This is not what you usually do, you should process as many rows as possible in every runQuery() invocation.
     */
    public static class OneRowTask extends CursorTask {

        private boolean queueTimeout;

        private OneRowTask() {
            super("foobar-queue");
        }

        private OneRowTask(boolean queueTimeOut) {
            this();
            this.queueTimeout = queueTimeOut;
        }

        @Override
        protected Cursor runQuery(Cursor startCursor) {
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            Query q = new Query(KIND);
            QueryResultIterator<Entity> iterator = ds.prepare(q).asQueryResultIterator(
                    startCursor == null? FetchOptions.Builder.withDefaults() : FetchOptions.Builder.withStartCursor(startCursor)
            );
            // there should be at least three rows to process
            assertTrue(iterator.hasNext());
            Entity e = iterator.next();
            e.setProperty("processed", true);
            ds.put(e);
            return iterator.hasNext()? iterator.getCursor() : null;
        }

        @Override
        protected boolean queueTimeOut() {
            return queueTimeout;
        }
    }

}
