/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The JSON Atomic Operations extension result body.
 */
@JsonInclude(Include.NON_NULL)
public class Results {

    @JsonProperty("atomic:results")
    private final List<Result> results;

    public List<Result> getResults() {
        return this.results;
    }

    @JsonCreator
    public Results(@JsonProperty("atomic:results") List<Result> results) {
        this.results = results;
    }
}
