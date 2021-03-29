/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import java.io.Serializable;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents a projected column as an alias in a query.
 */
public interface ColumnProjection extends Serializable {

    /**
     * Get the projection alias.
     *
     * @return alias
     */
    default String getAlias() {
        return getName();
    }

    /**
     * Get the safe alias (an alias that is not vulnerable to injection).
     *
     * @return an alias for column that is not vulnerable to injection
     */
    default String getSafeAlias() {
        String alias = getAlias();
        String name = getName();
        if (name.equals(alias)) {
            return name;
        } else {
            return createSafeAlias(name, alias);
        }
    }

    /**
     * Returns the name of the column.
     * @return the name of the column.
     */
    String getName();

    /**
     * Returns the query engine specific definition of the column.
     * @return the definition of the column.
     */
    String getExpression();

    /**
     * Returns the value type of the column.
     * @return the value type of the column.
     */
    ValueType getValueType();

    /**
     * Returns the column type of the column.
     * @return the column type of the column.
     */
    ColumnType getColumnType();

    /**
     * Get all arguments provided for this metric function.
     *
     * @return request arguments
     */
    default Map<String, Argument> getArguments() {
        return Collections.emptyMap();
    }

    // force implementations to define equals/hashCode
    boolean equals(Object other);
    int hashCode();

    /**
     * Creates an alias that is not vulnerable to injection.
     * @param name projected column's name
     * @param alias projected column's alias
     * @return an alias for projected column that is not vulnerable to injection
     */
    public static String createSafeAlias(String name, String alias) {
        return name + "_" + (Base64.getEncoder().encodeToString(alias.getBytes()).hashCode() & 0xfffffff);
    }

    /**
     * Whether or not a given projection can be nested into an inner query and outer query.
     * @return true if the projection can be nested.
     */
    default boolean canNest() {
        return true;
    }

    /**
     * While nesting, convert this projection into its outer query equivalent.
     * @param source The source of this projection.
     * @param lookupTable Used to answer questions about templated column definitions.
     * @param joinInOuter If possible, skip required joins in inner query and do the join in the outer query.
     * @return the outer projection.
     */
    ColumnProjection outerQuery(Queryable source, SQLReferenceTable lookupTable, boolean joinInOuter);

    /**
     * While nesting, convert this projection into its inner query equivalents.
     * @param source The source of this projection.
     * @param lookupTable Used to answer questions about templated column definitions.
     * @param joinInOuter If possible, skip required joins in inner query and do the join in the outer query.
     * @return the set of inner projections linked to the outer projection.
     */
    Set<ColumnProjection> innerQuery(Queryable source, SQLReferenceTable lookupTable, boolean joinInOuter);
}
