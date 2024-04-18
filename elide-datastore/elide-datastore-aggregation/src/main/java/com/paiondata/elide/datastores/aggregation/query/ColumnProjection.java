/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.query;

import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.paiondata.elide.datastores.aggregation.metadata.enums.ValueType;
import org.apache.commons.lang3.tuple.Pair;

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
        }
        return createSafeAlias(name, alias);
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
    @Override
    boolean equals(Object other);
    @Override
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
     * @param source The source of this projection.
     * @param metaDataStore MetaDataStore.
     * @return true if the projection can be nested.
     */
    default boolean canNest(Queryable source, MetaDataStore metaDataStore) {
        return true;
    }

    /**
     * Translate a column into outer and inner query columns for a two-pass aggregation.
     * @param source The source query of this projection.
     * @param metaDataStore MetaDataStore.
     * @param joinInOuter If possible, skip required joins in inner query and do the join in the outer query.
     * @return A pair consisting of the outer column projection and a set of inner column projections.
     */
    Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source,
                                                       MetaDataStore metaDataStore,
                                                       boolean joinInOuter);

    /**
     * Clones the projection and marks it as either projected or not projected.
     * @param projected Whether or not this projection should be returned in the result.
     * @param <T> The subclass of ColumnProjection.
     * @return The cloned column.
     */
    <T extends ColumnProjection> T withProjected(boolean projected);

    /**
     * Clones the projection with provided arguments.
     * @param arguments A map of String and {@link Argument}
     * @return The cloned column.
     */
    ColumnProjection withArguments(Map<String, Argument> arguments);

    /**
     * Returns whether or not this column is projected in the output (included in SELECT) or
     * only referenced in a filter expression.
     * @return True if part of the output projection.  False otherwise.
     */
    default boolean isProjected() {
        return true;
    }
}
