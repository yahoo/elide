/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Map;

/**
 * Model for representing JSON API meta information.
 */
@JsonAutoDetect
public class Meta extends KeyValMap {

    /**
     * Constructor.
     *
     * @param map Object containing meta information
     */
    public Meta(Map<String, Object> map) {
        super(map);
    }

    /**
     * Expose the meta map so that it will be included in the returned JSON-API document.
     *
     * @return the meta map
     */
    @JsonAnyGetter
    public Map<String, Object> getMetaMap() {
        return map;
    }
}
