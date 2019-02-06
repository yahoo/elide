/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

/**
 * The type Relation.
 */
public class Relation {

    /**
     * The Field.
     */
    final String field;
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
        this.field = field;
        this.resourceLinkages = resourceLinkages;
    }
}
