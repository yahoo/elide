/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine.schema;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.Schema;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromTable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A subclass of Schema that supports additional metadata to construct the FROM clause of a SQL query.
 */
@EqualsAndHashCode
@ToString
public class SQLSchema extends Schema {

    @Getter
    private String alias;

    @Getter
    private String tableDefinition;

    public SQLSchema(Class<?> entityClass, EntityDictionary dictionary) {
        super(entityClass, dictionary);

        FromTable fromTable = dictionary.getAnnotation(entityClass, FromTable.class);

        if (fromTable != null) {
            alias = fromTable.name();
            tableDefinition = fromTable.name();
        } else {
            FromSubquery fromSubquery = dictionary.getAnnotation(entityClass, FromSubquery.class);

            if (fromSubquery != null) {
                alias = "table" + this.hashCode();
                tableDefinition = fromSubquery.sql();
            } else {
                throw new IllegalStateException("Entity is missing FromTable or FromSubquery annotations");
            }
        }
    }
}
