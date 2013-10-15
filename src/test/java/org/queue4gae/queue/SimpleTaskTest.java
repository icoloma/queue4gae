package org.queue4gae.queue;


import org.junit.Test;

public class SimpleTaskTest {

    @Test
    public void testInheritance() {
        // Not really doing anything. This is how this fluent interface is intended to be used.
        // Probably breaking the Open/Close principle here, but I value simplicity over completeness.
        new InheritanceTask()
                // subclass methods first
                .foobar()

                // superclass methods come later
                .withTaskName("foo")
                .withTag("bar");
    }

    /**
     * Check that inheritance works transparently
     */
    public static class InheritanceTask extends InjectedTask {

        @Override
        public void run(QueueService queueService) {
            // do nothing
        }

        public InheritanceTask foobar() {
            return this;
        }

    }

}
