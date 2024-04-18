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
 * The JSON Atomic Operations extension entity body.
 */
@JsonInclude(Include.NON_NULL)
public class Operations {

    @JsonProperty("atomic:operations")
    private final List<Operation> operations;

    public List<Operation> getOperations() {
        return this.operations;
    }

    @JsonCreator
    public Operations(@JsonProperty("atomic:operations") List<Operation> operations) {
        this.operations = operations;
    }
}
