/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

/**
 * Used to inject all beans at time of construction.
 */
@FunctionalInterface
public interface Injector {

    /**
     * Inject an entity bean.
     *
     * @param entity Entity bean to inject
     */
    void inject(Object entity);
}
