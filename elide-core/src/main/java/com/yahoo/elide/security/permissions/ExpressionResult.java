/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions;

/**
 * Expression results.
 */
public enum ExpressionResult {
        PASS("PASSED"),
        FAIL("FAILED"),
        DEFERRED("WAS DEFERRED"),
        UNEVALUATED("WAS UNEVALUATED");

    private String name;

    ExpressionResult(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
