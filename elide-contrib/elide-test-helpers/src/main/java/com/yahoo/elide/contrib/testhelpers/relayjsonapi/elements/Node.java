/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attributes;

import java.util.LinkedHashMap;

/**
 * A Node represents an instance of entity in GraphQL JSON response.
 */
public class Node extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 7711282937931683426L;

    /**
     * Constructor.
     *
     * @param attributes  All field-value pairs representing a entity instance
     */
    public Node(Attributes attributes) {
        this.putAll(attributes);
    }
}
