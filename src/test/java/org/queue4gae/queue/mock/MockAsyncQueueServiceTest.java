package org.queue4gae.queue.mock;

import com.google.appengine.api.datastore.*;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.j4gae.GaeJacksonModule;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class MockAsyncQueueServiceTest {

    private static final Logger log = LoggerFactory.getLogger(MockAsyncQueueServiceTest.class);

    private MockAsyncQueueService queue;

    private LocalServiceTestHelper helper;

    @Before
    public void setupServices() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        objectMapper.registerModule(new GaeJacksonModule());

        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
        helper.setUp();

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Entity e = new Entity("foo", 1);
        ds.put(e);

        queue = new MockAsyncQueueService();
        queue.setInjectionService(new MockInjectionService());
        queue.setObjectMapper(objectMapper);
        queue.start();
    }

    @After
    public void tearDown() {
        queue.stop();
        helper.tearDown();
    }

    @Test
    public void testPost() throws Exception {
        for (int i = 0; i < 30; i ++) {
            queue.post(new MyTask(i));
        }
        queue.waitUntilEmpty(1000);
    }

    @Test
    public void testDelayedTasks() throws Exception {
        assertEquals(0, queue.getQueuedTaskCount());
        assertEquals(0, queue.getCompletedTaskCount());
        queue.post(new MyTask(0));
        queue.post(new MyTask(1));
        queue.post(new MyTask(1).withDelaySeconds(1));
        queue.waitUntilEmpty(1000);
        assertEquals(3, queue.getQueuedTaskCount());
        assertEquals(2, queue.getCompletedTaskCount());
        queue.runDelayedTasks();
        assertEquals(3, queue.getQueuedTaskCount());
        assertEquals(3, queue.getCompletedTaskCount());
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

    @Test
    public void testSetupTask() throws Exception {
        final AtomicInteger setupInvocations = new AtomicInteger();
        final AtomicInteger teardownInvocations = new AtomicInteger();

        queue.stop();
        queue = new MockAsyncQueueService() {

            @Override
            protected void setupTask(Task task) {
                setupInvocations.incrementAndGet();
            }

            @Override
            protected void teardownTask(Task task) {
                teardownInvocations.incrementAndGet();
            }

        };
        queue.setInjectionService(new MockInjectionService());
        queue.setObjectMapper(new ObjectMapper());
        queue.start();

        queue.post(new FailOnceTask());

        queue.waitUntilEmpty(2000);
        assertEquals(2, setupInvocations.get());
        assertEquals(2, teardownInvocations.get());
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
                DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
                Entity e = ds.get(KeyFactory.createKey("foo", 1));
                DatastoreServiceFactory.getDatastoreService().allocateIds("xyz", 1);

                log.info("Executing task #" + id);
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (EntityNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class FailOnceTask extends InjectedTask {

        private static ThreadLocal tl = new ThreadLocal();

        @Override
        public void run(QueueService queueService) {
            if (tl.get() == null) {
                tl.set(new Object());
                throw new RuntimeException("Temporary error");
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
