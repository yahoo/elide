/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.test.jsonapi.elements;

import com.google.gson.annotations.Expose;

import lombok.Getter;

/**
 * The type Relation.
 */
public class Relation {

    public static final boolean TO_ONE = true;
    public static final boolean TO_MANY = false;

    /**
     * The Field.
     */
    @Getter private final String field;

    @Expose(serialize = false)
    @Getter private final boolean toOne;

    /**
     * The Links.
     */
    @Getter private final Links links;

    /**
     * The Resource linkages.
     */
    @Getter private final ResourceLinkage[] resourceLinkages;

    /**
     * Instantiates a new Relation.
     *
     * @param field            the field
     * @param resourceLinkages the resource linkages
     */
    public Relation(String field, ResourceLinkage... resourceLinkages) {
        this(field, TO_MANY, null, resourceLinkages);
    }

    /**
     * Instantiates a new Relation.
     *
     * @param field            the field
     * @param links              the links
     * @param resourceLinkages the resource linkages
     */
    public Relation(String field, Links links, ResourceLinkage... resourceLinkages) {
        this(field, TO_MANY, links, resourceLinkages);
    }

    /**
     * Instantiates a new Relation.
     *
     * @param field            the field
     * @param toOne            whether or not the relation is toOne or toMany.
     * @param resourceLinkages the resource linkages
     */
    public Relation(String field, boolean toOne, Links links, ResourceLinkage... resourceLinkages) {
        this.field = field;
        this.toOne = toOne;
        this.links = links;
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
     * @param links             the links
     */
    public Relation(String field, Links links) {
        this(field, TO_MANY, links);
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
        this.links = null;
        this.resourceLinkages = new ResourceLinkage[0];
    }
}
