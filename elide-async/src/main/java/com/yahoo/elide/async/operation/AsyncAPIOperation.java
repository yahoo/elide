/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;

import java.util.concurrent.Callable;

/**
 * AsyncAPI Execute Operation Interface.
 * @param <T> Type of AsyncAPI.
 */
public interface AsyncAPIOperation<T extends AsyncAPI> extends Callable<AsyncAPIResult> {
}
