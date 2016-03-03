/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions;

import lombok.Getter;

/**
 * Expression results.
 */
public class ExpressionResult {
    @Getter private final Status status;
    @Getter private final String failureMessage;

    public static final ExpressionResult PASS_RESULT = new ExpressionResult(Status.PASS);
    public static final ExpressionResult DEFERRED_RESULT = new ExpressionResult(Status.DEFERRED);

    /**
     * Result status.
     */
    public enum Status {
        PASS,
        FAIL,
        DEFERRED
    }

    /**
     * Constructor.
     *
     * @param status Status to return
     * @param failureMessage Message associated with failure
     */
    public ExpressionResult(final Status status, final String failureMessage) {
        this.status = status;
        this.failureMessage = failureMessage;
    }

    /**
     * Constructor.
     *
     * @param status Status to return
     */
    public ExpressionResult(final Status status) {
        this(status, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(status);
        if (failureMessage != null) {
            sb.append(": ").append(failureMessage);
        }
        return sb.toString();
    }
}
