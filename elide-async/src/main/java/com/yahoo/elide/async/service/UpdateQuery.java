/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncQuery;

/**
 * Function which will be invoked for updating elide async query.
 */
@FunctionalInterface
public interface UpdateQuery {
    public void update(AsyncQuery query);
}
