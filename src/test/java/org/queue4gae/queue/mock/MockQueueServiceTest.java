package org.queue4gae.queue.mock;

import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.j4gae.GaeJacksonModule;
import org.junit.Before;
import org.junit.Test;
import org.queue4gae.queue.AbstractTask;
import org.queue4gae.queue.InjectedTask;
import org.queue4gae.queue.QueueService;

import static org.junit.Assert.*;

/**
 *
 */
public class MockQueueServiceTest {

    private MockQueueService queueService;

    static boolean aStarted;
    static boolean aFinished;
    static boolean bFinished;

    @Before
    public void setupServices() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        objectMapper.registerModule(new GaeJacksonModule());

        queueService = new MockQueueService();
        queueService.setInjectionService(new MockInjectionService());
        queueService.setObjectMapper(objectMapper);
    }

    @Test
    public void recursiveTasks() {
        aStarted = aFinished = bFinished = false;
        queueService.post(new ATask());
        assertTrue(aStarted && aFinished);
        assertTrue(bFinished);
    }

    @Test
    public void tombstone() {
        queueService.post(new TombstonedTask().withTaskName("foo"));
        try {
            queueService.post(new TombstonedTask().withTaskName("foo"));
            fail("Accepted a tomstoned task");
        } catch (TaskAlreadyExistsException e) {
            assertEquals(1, queueService.getQueuedTaskCount());
        }
    }

    @Test
    public void testDelayedTasksExecutionOrder() {
        DelayedTask later = new DelayedTask("later").withDelaySeconds(100);
        queueService.post(later);

        DelayedTask sooner = new DelayedTask("sooner").withDelaySeconds(10);
        queueService.post(sooner);

        queueService.runDelayedTasks();
        assertTrue("later".equals(DelayedTask.lastValue));
    }

    @Test
    public void testRetries() {
        queueService.withRetries(2);
        queueService.post(new FailOnceTask());
    }

    public static class ATask extends InjectedTask {

        @Override
        public void run(QueueService queueService) {
            aStarted = true;
            queueService.post(new BTask());
            aFinished = true;
        }

    }

    public static class BTask extends InjectedTask {

        @Override
        public void run(QueueService queueService) {
            assertTrue(aStarted);
            assertTrue(aFinished);
            bFinished = true;
        }

    }

    public static class TombstonedTask extends InjectedTask {

        @Override
        public void run(QueueService queueService) {
        }

    }

    public static class DelayedTask extends AbstractTask<DelayedTask> {

        public static String lastValue;

        private String myValue;

        private DelayedTask() {
        }

        public DelayedTask(String myValue) {
            this.myValue = myValue;
        }

        @Override
        public void run(QueueService queueService) {
            try {
                lastValue = myValue;
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class FailOnceTask extends InjectedTask {

        private static boolean failed;

        @Override
        public void run(QueueService queueService) {
            if (!failed) {
                failed = true;
                throw new RuntimeException("Temporary error");
            }
        }

    }



}
