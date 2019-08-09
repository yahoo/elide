/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * {@link Edges} is a list of {@link Node}s
 */
public class Edges extends LinkedList<Map<String, Object>> {

    private static final long serialVersionUID = 7587614051331106241L;

    /**
     * Constructor.
     *
     * @param nodes  The list of {@link Node}s
     */
    public Edges(List<Node> nodes) {
        if (nodes != null && !nodes.isEmpty()) {
            nodes.forEach(node -> {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("node", node);
                this.add(attributes);
            });
        }
    }
}
