package org.queue4gae.queue.mock;

import org.queue4gae.queue.InjectionService;

/**
 * This implementation does not do anything. Use this in your mock environment, or if you simply do not need to
 * inject anything into Task classes.
 */
public class MockInjectionService implements InjectionService {

    @Override
    public void injectMembers(Object instance) {
        // doing nothing is easy. This line is 100% bug-proof
    }

}
