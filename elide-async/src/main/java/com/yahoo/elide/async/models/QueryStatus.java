/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

/**
 * ENUM of possible query statuses.
 */
public enum QueryStatus {
    COMPLETE,
    QUEUED,
    PROCESSING,
    CANCELLED,
    TIMEDOUT,
    FAILURE,
    CANCEL_COMPLETE
}
