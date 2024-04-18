/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

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
     * @param links         the links
     * @param relationships the relationships
     */
    public Resource(Id id, Type type, Attributes attributes, Links links, Relationships relationships) {
        super(id, type);
        if (attributes != null) {
            this.put("attributes", attributes);
        }
        if (relationships != null) {
            this.put("relationships", relationships);
        }
        if (links != null) {
            this.put("links", links);
        }
    }

    /**
     * Instantiates a new Resource.
     *
     * @param lid           the lid
     * @param type          the type
     * @param attributes    the attributes
     * @param links         the links
     * @param relationships the relationships
     */
    public Resource(Lid lid, Type type, Attributes attributes, Links links, Relationships relationships) {
        super(lid, type);
        if (attributes != null) {
            this.put("attributes", attributes);
        }
        if (relationships != null) {
            this.put("relationships", relationships);
        }
        if (links != null) {
            this.put("links", links);
        }
    }
}
