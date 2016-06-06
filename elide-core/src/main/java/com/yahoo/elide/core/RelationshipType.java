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
    NONE(false, false, false),
    ONE_TO_ONE(false, true, false),
    ONE_TO_MANY(true, false, false),
    MANY_TO_ONE(false, true, false),
    MANY_TO_MANY(true, false, false),
    COMPUTED_NONE(false, false, true),
    COMPUTED_ONE_TO_ONE(false, true, true),
    COMPUTED_ONE_TO_MANY(true, false, true),
    COMPUTED_MANY_TO_ONE(false, true, true),
    COMPUTED_MANY_TO_MANY(true, false, true);

    private final boolean toMany;
    private final boolean toOne;
    private final boolean isComputed;

    RelationshipType(boolean toMany, boolean toOne, boolean isComputed) {
        this.toMany = toMany;
        this.toOne = toOne;
        this.isComputed = isComputed;
    }

    public boolean isToMany() {
        return toMany;
    }

    public boolean isToOne() {
        return toOne;
    }

    public boolean isComputed() {
        return isComputed;
    }
}
