/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import static com.yahoo.elide.modelconfig.model.NamespaceConfig.DEFAULT;
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
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    "namespace",
    "joins",
    "measures",
    "dimensions",
    "tags",
    "hints",
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
@NoArgsConstructor
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

    @Builder.Default
    @JsonProperty("isFact")
    private Boolean isFact = true;

    @Builder.Default
    @JsonProperty("hidden")
    private Boolean hidden = false;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("filterTemplate")
    private String filterTemplate;

    @JsonProperty("cardinality")
    private String cardinality;

    @Builder.Default
    @JsonProperty("readAccess")
    private String readAccess = "Prefab.Role.All";

    @Builder.Default
    @JsonProperty("namespace")
    private String namespace = DEFAULT;

    @JsonProperty("joins")
    @Singular
    private List<Join> joins = new ArrayList<>();

    @JsonProperty("measures")
    @Singular
    private List<Measure> measures = new ArrayList<>();

    @JsonProperty("dimensions")
    @Singular
    private List<Dimension> dimensions = new ArrayList<>();

    @Builder.Default
    @JsonProperty("tags")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> tags = new LinkedHashSet<>();

    @Builder.Default
    @JsonProperty("hints")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> hints = new LinkedHashSet<>();

    @JsonProperty("arguments")
    @Singular
    private List<Argument> arguments = new ArrayList<>();

    @JsonProperty("extend")
    private String extend;

    @JsonProperty("sql")
    private String sql;

    @JsonProperty("table")
    private String table;

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
    public Column getField(String fieldName) {
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

    public Argument getArgument(String argName) {
        return this.arguments.stream()
                        .filter(arg -> arg.getName().equals(argName))
                        .findFirst()
                        .orElse(null);
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
        return elideTableConfig.getTable(getGlobalExtend());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGlobalName() {
        return getModelName(name, namespace);
    }

    /**
     * Return the globally unique table name for the inherited table.
     * @return The globally unique name for the inherited table or null.
     */
    public String getGlobalExtend() {
        if (extend == null || extend.isEmpty()) {
            return extend;
        }
        return getModelName(extend, namespace);
    }

    public static String getModelName(String tableName, String namespace) {
        if (namespace == null || namespace.isEmpty() || namespace.equals(DEFAULT)) {
            return tableName;
        }

        return namespace + "_" + tableName;
    }

    /**
     * Get the list of {@link Column} defined for this table.
     * @return List of {@link Column} defined for this table.
     */
    public List<Column> getColumns() {
        return Stream.of(dimensions, measures, joins)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
    }
}
