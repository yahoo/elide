/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncAPI;

/**
 * Function which will be invoked for updating elide async query base implementation.
 * @param <T> AsyncQueryBase Type Implementation.
 */
@FunctionalInterface
public interface UpdateQuery<T extends AsyncAPI> {
    public void update(T query);
}
