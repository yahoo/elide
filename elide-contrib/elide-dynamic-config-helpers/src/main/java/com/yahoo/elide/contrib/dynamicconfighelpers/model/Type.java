/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.model;

/**
 * Data Type of the field.
 */
public enum Type {

    TIME("TIME"),
    INTEGER("INTEGER"),
    DECIMAL("DECIMAL"),
    MONEY("MONEY"),
    TEXT("TEXT"),
    COORDINATE("COORDINATE"),
    BOOLEAN("BOOLEAN");

    private final String value;

    private Type(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
