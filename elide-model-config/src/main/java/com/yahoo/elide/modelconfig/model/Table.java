/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Streams;

import org.apache.commons.lang3.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Table Model JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "friendlyName",
    "schema",
    "isFact",
    "hidden",
    "description",
    "cardinality",
    "readAccess",
    "joins",
    "measures",
    "dimensions",
    "tags",
    "arguments",
    "extend",
    "sql",
    "table",
    "dbConnectionName",
    "filterTemplate"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@Builder
public class Table implements Named {

    @JsonProperty("name")
    private String name;

    @JsonProperty("friendlyName")
    private String friendlyName;

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("dbConnectionName")
    private String dbConnectionName;

    @JsonProperty("isFact")
    private Boolean isFact;

    @JsonProperty("hidden")
    private Boolean hidden;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("filterTemplate")
    private String filterTemplate;

    @JsonProperty("cardinality")
    private String cardinality;

    @JsonProperty("readAccess")
    private String readAccess;

    @JsonProperty("joins")
    @Singular
    private List<Join> joins;

    @JsonProperty("measures")
    @Singular
    private List<Measure> measures;

    @JsonProperty("dimensions")
    @Singular
    private List<Dimension> dimensions;

    @JsonProperty("tags")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> tags;

    @JsonProperty("arguments")
    @Singular
    private List<Argument> arguments;

    @JsonProperty("extend")
    private String extend;

    @JsonProperty("sql")
    private String sql;

    @JsonProperty("table")
    private String table;

    public Table() {
        this.isFact = true;
        this.hidden = false;
        this.readAccess = "Prefab.Role.All";
        this.joins = new ArrayList<>();
        this.measures = new ArrayList<>();
        this.dimensions = new ArrayList<>();
        this.tags = new LinkedHashSet<>();
        this.arguments = new ArrayList<>();
    }

    /**
     * Returns description of the table object.
     * If null, returns the name.
     * @return description
     */
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }

    /**
     * Checks if this model has provided field.
     * @param fieldName Name of the {@link Dimension} or {@link Measure} to check for.
     * @return true if this model has provided field.
     */
    public boolean hasField(String fieldName) {
        return hasName(this.dimensions, fieldName) || hasName(this.measures, fieldName);
    }

    /**
     * Provides the Field details for provided field name.
     * @param fieldName Name of {@link Dimension} or {@link Measure} to retrieve.
     * @return Field for provided field name.
     */
    public Named getField(String fieldName) {
        return Streams.concat(this.dimensions.stream(), this.measures.stream())
                        .filter(col -> col.getName().equals(fieldName))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Checks if this model has provided argument.
     * @param argName Name of the {@link Argument} to  check for.
     * @return true if this model has provided argument.
     */
    public boolean hasArgument(String argName) {
        return hasName(this.arguments, argName);
    }

    /**
     * Checks if this model has provided join field.
     * @param joinName Name of the {@link Join} to check for.
     * @return true if this model has provided join field.
     */
    public boolean hasJoinField(String joinName) {
        return hasName(this.joins, joinName);
    }

    /**
     * Provides the Join details for provided join name.
     * @param joinName Name of the {@link Join} to retrieve.
     * @return Join for provided join name.
     */
    public Join getJoin(String joinName) {
        return this.joins.stream()
                        .filter(join -> join.getName().equals(joinName))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Checks if this model has a parent model.
     * @return true if this model extends another model
     */
    public boolean hasParent() {
        return StringUtils.isNotBlank(this.extend);
    }

    /**
     * Provides the parent model for this model.
     * @param elideTableConfig {@link ElideTableConfig}
     * @return Parent model for this model
     */
    public Table getParent(ElideTableConfig elideTableConfig) {
        return elideTableConfig.getTable(this.extend);
    }
}
