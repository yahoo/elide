/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.dao;

import com.paiondata.elide.async.models.AsyncApi;

/**
 * Function which will be invoked for updating elide async query base implementation.
 * @param <T> AsyncQueryBase Type Implementation.
 */
@FunctionalInterface
public interface UpdateQuery<T extends AsyncApi> {
    public void update(T query);
}
