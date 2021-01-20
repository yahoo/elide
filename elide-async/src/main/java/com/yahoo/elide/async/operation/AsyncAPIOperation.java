/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.User;

/**
 * AsyncAPI Execute Operation Interface.
 * @param <T> Type of AsyncAPI.
 */
public interface AsyncAPIOperation<T extends AsyncAPI> {

    /**
     * Execute the AsyncAPI request.
     * @param queryObj AsyncAPI type object.
     * @param scope RequestScope object.
     * @param user User object.
     * @param service AsyncExecutorService instance.
     * @param apiVersion API Version.
     * @return AsyncAPIResult object
     */
    public abstract AsyncAPIResult execute(AsyncAPI queryObj, RequestScope scope,
            User user, AsyncExecutorService service, String apiVersion);
}
