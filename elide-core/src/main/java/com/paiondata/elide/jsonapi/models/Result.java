/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The JSON Atomic Operations extension result body.
 */
public class Result {
    private final Resource data;
    private final Meta meta;

    public Resource getData() {
        return this.data;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Meta getMeta() {
        return this.meta;
    }

    @JsonCreator
    public Result(@JsonProperty("data") Resource data, @JsonProperty("meta") Meta meta) {
        this.data = data;
        this.meta = meta;
    }

    public Result(Resource data) {
        this.data = data;
        this.meta = null;
    }
}
