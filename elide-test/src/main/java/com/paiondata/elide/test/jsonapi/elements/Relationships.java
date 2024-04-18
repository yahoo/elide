/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

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
            if (relation.getLinks() != null) {
                data.put("links", relation.getLinks());
            }
            if (relation.isToOne()) {
                if (relation.getResourceLinkages().length == 0) {
                    data.put("data", null);
                } else {
                    data.put("data", relation.getResourceLinkages()[0]);
                }
            } else {
                data.put("data", relation.getResourceLinkages());
            }
            this.put(relation.getField(), data);
        }
    }
}
