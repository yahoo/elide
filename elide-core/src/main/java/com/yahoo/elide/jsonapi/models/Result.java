/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The JSON Atomic Operations extension result body.
 */
public class Result {
    private final Resource data;

    public Resource getData() {
        return this.data;
    }

    @JsonCreator
    public Result(@JsonProperty("data") Resource data) {
        this.data = data;
    }
}
