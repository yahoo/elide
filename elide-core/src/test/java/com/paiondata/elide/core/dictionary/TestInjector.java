/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.dictionary;

/**
 * Test Dependency Injector.
 */
public class TestInjector implements Injector {

    public TestInjector() {
    }

    @Override
    public void inject(Object entity) {
        //noop
    }

    @Override
    public <T> T instantiate(Class<T> cls) {
        try {
            return (T) cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
