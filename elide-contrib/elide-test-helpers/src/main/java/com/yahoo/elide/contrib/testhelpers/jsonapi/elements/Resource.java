/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

/**
 * Holds a Resource.
 */
public class Resource extends ResourceLinkage {

    /**
     * Instantiates a new Resource.
     *
     * @param id            the id
     * @param type          the type
     * @param attributes    the attributes
     * @param relationships the relationships
     */
    public Resource(Id id, Type type, Attributes attributes, Relationships relationships) {
        super(id, type);
        this.put("attributes", attributes);
        this.put("relationships", relationships);
    }
}
