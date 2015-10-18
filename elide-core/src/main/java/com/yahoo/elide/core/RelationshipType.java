/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

/**
 * Relationship types.
 */
public enum RelationshipType {
    NONE(false, false),
    ONE_TO_ONE(false, true),
    ONE_TO_MANY(true, false),
    MANY_TO_ONE(false, true),
    MANY_TO_MANY(true, false);

    private final boolean toMany;
    private final boolean toOne;

    RelationshipType(boolean toMany, boolean toOne) {
        this.toMany = toMany;
        this.toOne = toOne;
    }

    public boolean isToMany() {
        return toMany;
    }

    public boolean isToOne() {
        return toOne;
    }
}
