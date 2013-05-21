package org.queue4gae.guice;

import com.google.inject.Injector;
import org.queue4gae.queue.InjectionService;

import javax.inject.Inject;

/**
 * InjectionService implementation for Guice
 */
public class GuiceInjectionService implements InjectionService {

    private Injector injector;

    @Override
    public void injectMembers(Object instance) {
        injector.injectMembers(instance);
    }

    @Inject
    public void setInjector(Injector injector) {
        this.injector = injector;
    }

}
