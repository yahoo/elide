/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The type Relationships.
 */
public class Relationships extends LinkedHashMap<String, Map<String, ?>> {

    /**
     * Instantiates a new Relationships.
     *
     * @param relations the relations
     */
    public Relationships(Relation... relations) {
        for (Relation relation : relations) {
            Map<String, Object> data = new LinkedHashMap<>();
            if (relation.links != null) {
                data.put("links", relation.links);
            }
            if (relation.toOne) {
                if (relation.resourceLinkages.length == 0) {
                    data.put("data", null);
                } else {
                    data.put("data", relation.resourceLinkages[0]);
                }
            } else {
                data.put("data", relation.resourceLinkages);
            }
            this.put(relation.field, data);
        }
    }
}
