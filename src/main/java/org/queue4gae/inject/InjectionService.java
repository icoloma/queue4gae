package org.queue4gae.inject;

/**
 * Delegates in your favorite DI container the messy task of Dependency Injection
 */
public interface InjectionService {

    /**
     * Injects fields annotated with javax.inject.Inject.
     * @param instance The object with the fields to inject.
     */
    public void injectFields(Object instance);

}
