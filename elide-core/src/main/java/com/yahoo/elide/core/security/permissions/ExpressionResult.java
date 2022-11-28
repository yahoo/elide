/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.permissions;

import static org.fusesource.jansi.Ansi.ansi;

import org.fusesource.jansi.Ansi;

/**
 * Expression results.
 */
public enum ExpressionResult {
        PASS("PASSED", Ansi.Color.GREEN),
        FAIL("FAILED", Ansi.Color.RED),
        DEFERRED("WAS DEFERRED", Ansi.Color.YELLOW),
        UNEVALUATED("WAS UNEVALUATED", Ansi.Color.BLUE);

    private final String name;
    private final Ansi.Color color;

    ExpressionResult(String name, Ansi.Color color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String toString() {
        return ansi().fg(color).a(name).reset().toString();
    }
}
