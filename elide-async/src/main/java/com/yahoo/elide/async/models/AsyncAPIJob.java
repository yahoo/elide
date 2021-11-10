/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.core.security.User;

import lombok.Data;

import java.util.concurrent.CountDownLatch;

/**
 * Model Class for AsyncAPI Job.
 */
@Data
public class AsyncAPIJob {
    User user;
    AsyncAPI asyncApi;
    CountDownLatch done = new CountDownLatch(1);

    public AsyncAPIJob(AsyncAPI asyncAPI, User user) {
        this.user = user;
        this.asyncApi = asyncAPI;
    }
}
