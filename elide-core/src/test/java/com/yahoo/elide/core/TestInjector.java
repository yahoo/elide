/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.Injector;

import javax.inject.Inject;

/**
 * Test Dependency Injector.
 */
public class TestInjector implements Injector {
    private final com.google.inject.Injector injector;

    @Inject
    public TestInjector(com.google.inject.Injector injector) {
        this.injector = injector;
    }

    @Override
    public void inject(Object entity) {
        injector.injectMembers(entity);
    }

    @Override
    public <T> T instantiate(Class<T> cls) {
        return injector.getInstance(cls);
    }
}
