package org.queue4gae.queue.mock;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.j4gae.GaeJacksonModule;
import org.j4gae.ObjectMapperSetup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.queue4gae.queue.InjectedTask;
import org.queue4gae.queue.QueueService;
import org.queue4gae.queue.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class MockAsyncQueueServiceTest {

    private static final Logger log = LoggerFactory.getLogger(MockAsyncQueueServiceTest.class);

    private MockAsyncQueueService queue;

    @Before
    public void setupServices() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        objectMapper.registerModule(new GaeJacksonModule());

        LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
        queue = new MockAsyncQueueService(helper);
        queue.setInjectionService(new MockInjectionService());
        queue.setObjectMapper(objectMapper);
    }

    @Test
    public void testPost() throws Exception {
        for (int i = 0; i < 30; i ++) {
            queue.post(new MyTask(i));
        }
        queue.waitUntilEmpty(1000);
    }

    @Test
    public void testFailingTask() throws Exception {
        for (int i = 0; i < 5; i ++) {
            queue.post(new MyTask(i));
        }
        queue.post(new FailingTask());
        try {
            queue.waitUntilEmpty(100);
            Assert.fail("Undetected failing task");
        } catch (TimeoutException e) {
            assertEquals(5, queue.getCompletedTaskCount());
        }
    }

    public static class MyTask extends InjectedTask {

        private int id;

        private MyTask(int id) {
            this.id = id;
        }

        private MyTask() {
            // for jackson
        }

        @Override
        public void run(QueueService queueService) {
            try {
                // do something with the Datastore to check that this thread can work with AppEngine
                DatastoreServiceFactory.getDatastoreService().allocateIds("xyz", 1);

                log.info("Executing task #" + id);
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class FailingTask extends InjectedTask {

        @Override
        public void run(QueueService queueService) {
            throw new RuntimeException("bazzinga!");
        }
    }
}
