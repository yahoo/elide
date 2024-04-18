/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects;

import com.paiondata.elide.core.filter.Operator;
import com.paiondata.elide.datastores.aggregation.annotation.JoinType;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite.SupportedAggregation;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite.SupportedOperation;
import com.paiondata.elide.datastores.aggregation.timegrains.Time;
import com.paiondata.elide.datastores.jpql.filter.JPQLPredicateGenerator;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlDialect;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface for SQL Dialects used to customize SQL queries for specific persistent storage.
 */
public interface SQLDialect {

    /**
     * Returns the name of the Dialect.
     * @return dialect name.
     */
    String getDialectType();

    /**
     * Checks whether we need to use alias for orderby.
     * @return boolean.
     */
    boolean useAliasForOrderByClause();

    /**
     * Add "AS" before table/subquery aliases in generated SQL.
     * @return boolean.
     */
    boolean useASBeforeTableAlias();

    /**
     * Generates required offset and limit clause.
     * @param offset position of the first record.
     * @param limit maximum number of record.
     * @return the offset and limit clause.
     */
    String generateOffsetLimitClause(int offset, int limit);

    /**
     * Provides begin quote required for SQL identifiers.
     * @return begin quote for SQL identifiers.
     */
    char getBeginQuote();

    /**
     * Provides end quote required for SQL identifiers.
     * @return end quote for SQL identifiers.
     */
    char getEndQuote();

    /**
     * Provides keyword for requested Join Type.
     * @param joinType {@link JoinType} enum
     * @return the keyword for provided Join type.
     */
    String getJoinKeyword(JoinType joinType);

    /**
     * Translates Elide's {@link Time} object to the native JDBC date/time object supported
     * by the underlying driver.
     *
     * @param time The elide time object.
     * @return A type compatible with JDBC.
     */
    default Object translateTimeToJDBC(Time time) {
        if (time.isSupportsHour()) {
            return new java.sql.Timestamp(time.getTime());
        }
        return new java.sql.Date(time.getTime());
    }

    /**
     * Fetches the Calcite dialect associated with this Elide dialect.
     * @return Calcite dialect
     */
    default SqlDialect getCalciteDialect() {
        String quotes = String.valueOf(getBeginQuote());
        return new SqlDialect(SqlDialect.EMPTY_CONTEXT
                .withIdentifierQuoteString(quotes)
                .withCaseSensitive(true)
                .withQuotedCasing(Casing.UNCHANGED)
                .withUnquotedCasing(Casing.UNCHANGED));
    }

    /**
     * Fetch the aggregation for the given SQL function name or NULL if not supported.
     * @param name The name (case insensitive) of the aggregation function or UDF.
     * @return The supported aggregation or NULL if not supported.
     */
    SupportedAggregation getSupportedAggregation(String name);

    /**
     * Fetch the operation for the given SQL function name or NULL if not supported.
     * @param name The name (case insensitive) of the operation or UDF.
     * @return The supported operation or NULL if not supported.
     */
    SupportedOperation getSupportedOperation(String name);

    /**
     * Returns a map of filter predicate operators to SQL/JPQL expression generators that
     * will override the system defaults.
     * @return SQL/JPQL expression generator overrides.
     */
    default Map<Operator, JPQLPredicateGenerator> getPredicateGeneratorOverrides() {
        return new HashMap<>();
    }
}
