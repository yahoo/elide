/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.SupportedAggregation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.SupportedOperation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Common code for {@link SQLDialect} implementations.
 */
public abstract class AbstractSqlDialect implements SQLDialect {

    public static final String OFFSET = "OFFSET ";
    public static final String LIMIT = "LIMIT ";
    public static final char BACKTICK = '`';
    public static final char DOUBLE_QUOTE = '"';
    public static final char SPACE = ' ';
    public static final char COMMA = ',';
    public static final String LEFT_JOIN_SYNTAX = "LEFT OUTER JOIN";
    public static final String INNER_JOIN_SYNTAX = "INNER JOIN";
    public static final String FULL_JOIN_SYNTAX = "FULL OUTER JOIN";
    public static final String CROSS_JOIN_SYNTAX = "CROSS JOIN";

    protected HashMap<String, SupportedAggregation> supportedAggregations;
    protected HashMap<String, SupportedOperation> supportedOperations;

    protected static SupportedOperation[] STANDARD_OPERATIONS = {
            SupportedOperation.builder().name("/").build(),
            SupportedOperation.builder().name("+").build(),
            SupportedOperation.builder().name("-").build(),
            SupportedOperation.builder().name("*").build(),
            SupportedOperation.builder().name("%").build(),
            SupportedOperation.builder().name("MOD").build(),
            SupportedOperation.builder().name("UNION").build(),
            SupportedOperation.builder().name("INTERSECT").build(),
            SupportedOperation.builder().name("INTERSECT").build(),
            SupportedOperation.builder().name("IN").build(),
            SupportedOperation.builder().name("NOT IN").build(),
            SupportedOperation.builder().name(">").build(),
            SupportedOperation.builder().name("<").build(),
            SupportedOperation.builder().name(">=").build(),
            SupportedOperation.builder().name("<=").build(),
            SupportedOperation.builder().name("=").build(),
            SupportedOperation.builder().name("<>").build(),
            SupportedOperation.builder().name("OR").build(),
            SupportedOperation.builder().name("AND").build(),
            SupportedOperation.builder().name("NOT").build(),
            SupportedOperation.builder().name("LIKE").build(),
            SupportedOperation.builder().name("BETWEEN").build(),
            SupportedOperation.builder().name("CASE").build(),
            SupportedOperation.builder().name("COALESCE").build(),
            SupportedOperation.builder().name("EXISTS").build(),
            SupportedOperation.builder().name("IS NULL").build(),
            SupportedOperation.builder().name("IS NOT NULL").build(),
            SupportedOperation.builder().name("IS TRUE").build(),
            SupportedOperation.builder().name("IS NOT TRUE").build(),
            SupportedOperation.builder().name("IS FALSE").build(),
            SupportedOperation.builder().name("IS NOT FALSE").build(),
            SupportedOperation.builder().name("CAST").build(),
            SupportedOperation.builder().name("FLOOR").build(),
            SupportedOperation.builder().name("CEILING").build(),
            SupportedOperation.builder().name("TRIM").build()
    };

    protected static SupportedAggregation[] STANDARD_AGGREGATIONS = {
            SupportedAggregation.builder()
                    .name("SUM")
                    .innerTemplate("SUM(%s)")
                    .outerTemplate("SUM(%s)")
                    .build(),
            SupportedAggregation.builder()
                    .name("COUNT")
                    .innerTemplate("COUNT(%s)")
                    .outerTemplate("COUNT(%s)")
                    .build(),
            SupportedAggregation.builder()
                    .name("MAX")
                    .innerTemplate("MAX(%s)")
                    .outerTemplate("MAX(%s)")
                    .build(),
            SupportedAggregation.builder()
                    .name("MIN")
                    .innerTemplate("MIN(%s)")
                    .outerTemplate("MIN(%s)")
                    .build(),
            SupportedAggregation.builder()
                    .name("AVG")
                    .innerTemplate("SUM(%1$s)")
                    .innerTemplate("COUNT(%1$s)")
                    .outerTemplate("SUM(%1$s)/COUNT(%1$s)")
                    .build(),
            SupportedAggregation.builder()
                    .name("VAR_POP")
                    .innerTemplate("SUM(%1$s * %1$s)")
                    .innerTemplate("SUM(%1$s)")
                    .innerTemplate("COUNT(%1$s)")
                    .outerTemplate("(SUM(%1$s) - SUM(%2$s) * SUM(%2$s) / COUNT(%3$s)) / COUNT(%3$s)")
                    .build()
    };

    public AbstractSqlDialect() {
        supportedAggregations = Arrays.stream(STANDARD_AGGREGATIONS)
                .collect(Collectors.toMap(SupportedAggregation::getName, Function.identity(),
                        (a, b) -> a, HashMap::new));

        supportedOperations = Arrays.stream(STANDARD_OPERATIONS)
                .collect(Collectors.toMap(SupportedOperation::getName, Function.identity(),
                        (a, b) -> a, HashMap::new));
    }

    @Override
    public boolean useAliasForOrderByClause() {
        return false;
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        return OFFSET + offset + SPACE + LIMIT + limit;
    }

    @Override
    public char getBeginQuote() {
        return BACKTICK;
    }

    @Override
    public char getEndQuote() {
        return BACKTICK;
    }

    @Override
    public String getJoinKeyword(JoinType joinType) {

        switch (joinType) {
            case LEFT:
                return getLeftJoinKeyword();
            case INNER:
                return getInnerJoinKeyword();
            case FULL:
                return getFullJoinKeyword();
            case CROSS:
                return getCrossJoinKeyword();
            default:
                return getLeftJoinKeyword();
        }
    }

    public String getLeftJoinKeyword() {
        return LEFT_JOIN_SYNTAX;
    }

    public String getInnerJoinKeyword() {
        return INNER_JOIN_SYNTAX;
    }

    public String getFullJoinKeyword() {
        return FULL_JOIN_SYNTAX;
    }

    public String getCrossJoinKeyword() {
        return CROSS_JOIN_SYNTAX;
    }

    @Override
    public SupportedAggregation getSupportedAggregation(String name) {
        return supportedAggregations.getOrDefault(name.toUpperCase(Locale.ENGLISH), null);
    }

    @Override
    public SupportedOperation getSupportedOperation(String name) {
        return supportedOperations.getOrDefault(name.toUpperCase(Locale.ENGLISH), null);
    }
}
