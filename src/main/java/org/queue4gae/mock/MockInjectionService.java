package org.queue4gae.mock;

import org.queue4gae.inject.InjectionService;

/**
 * This implementation is intended to test your Task classes.
 */
public class MockInjectionService implements InjectionService {

    @Override
    public void injectFields(Object instance) {
        // nothing
    }

}
