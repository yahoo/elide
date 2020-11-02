/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Elide DB POJO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "dbconfigs"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class ElideDBConfig {

    @JsonProperty("dbconfigs")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<DBConfig> dbconfigs = new LinkedHashSet<DBConfig>();
}
