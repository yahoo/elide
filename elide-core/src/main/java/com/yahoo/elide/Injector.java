/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

/**
 * Abstraction around dependency injection.
 */
@FunctionalInterface
public interface Injector {

    /**
     * Inject an elide object.
     *
     * @param entity object to inject
     */
    void inject(Object entity);

    /**
     * Instantiates a new instance of a class using the DI framework.
     *
     * @param cls The class to instantiate.
     * @return An instance of the class.
     */
    default <T> T instantiate(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
