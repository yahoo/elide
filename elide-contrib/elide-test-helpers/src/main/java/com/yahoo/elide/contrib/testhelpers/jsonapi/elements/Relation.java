/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import com.google.gson.annotations.Expose;

/**
 * The type Relation.
 */
public class Relation {

    public static final boolean TO_ONE = true;
    public static final boolean TO_MANY = false;

    /**
     * The Field.
     */
    final String field;

    @Expose(serialize = false)
    final boolean toOne;

    /**
     * The Resource linkages.
     */
    final ResourceLinkage[] resourceLinkages;

    /**
     * Instantiates a new Relation.
     *
     * @param field            the field
     * @param resourceLinkages the resource linkages
     */
    public Relation(String field, ResourceLinkage... resourceLinkages) {
        this(field, TO_MANY, resourceLinkages);
    }

    /**
     * Instantiates a new Relation.
     *
     * @param field            the field
     * @param toOne            whether or not the relation is toOne or toMany.
     * @param resourceLinkages the resource linkages
     */
    public Relation(String field, boolean toOne, ResourceLinkage... resourceLinkages) {
        this.field = field;
        this.toOne = toOne;
        this.resourceLinkages = resourceLinkages;
    }

    /**
     * Instantiates a new Relation.
     *
     * @param field            the field
     */
    public Relation(String field) {
        this(field, TO_MANY);
    }

    /**
     * Instantiates a new Relation.
     *
     * @param field            the field
     * @param toOne            whether or not the relation is toOne or toMany.
     */
    public Relation(String field, boolean toOne) {
        this.field = field;
        this.toOne = toOne;
        this.resourceLinkages = new ResourceLinkage[0];
    }
}
