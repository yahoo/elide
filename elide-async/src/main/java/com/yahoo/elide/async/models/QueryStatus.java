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
    FAILURE
}