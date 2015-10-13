/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Model for representing JSON API meta information
 */
public class Meta extends KeyValMap {
    /**
     * Constructor
     *
     * @param meta Object containing meta information
     */
    private Meta(@JsonProperty("meta") Map<String, Object> meta) {
        super(meta);
    }
}
