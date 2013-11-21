package org.queue4gae.queue.mock;

import com.google.appengine.api.datastore.Cursor;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.j4gae.GaeJacksonModule;
import org.j4gae.ObjectMapperSetup;
import org.junit.Before;
import org.junit.Test;
import org.queue4gae.queue.InjectedTask;
import org.queue4gae.queue.QueueService;
import org.queue4gae.queue.mock.MockInjectionService;
import org.queue4gae.queue.mock.MockQueueService;

import static org.junit.Assert.assertTrue;

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



}
