/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects;

import com.paiondata.elide.datastores.aggregation.annotation.JoinType;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite.SupportedAggregation;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite.SupportedOperation;

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

    //TODO - Break this out by dialect and add other dialect specific operators.
    protected static SupportedOperation[] STANDARD_OPERATIONS = {
            SupportedOperation.builder().name("/").build(),
            SupportedOperation.builder().name("+").build(),
            SupportedOperation.builder().name("-").build(),
            SupportedOperation.builder().name("*").build(),
            SupportedOperation.builder().name("%").build(),
            SupportedOperation.builder().name("UNION").build(),
            SupportedOperation.builder().name("INTERSECT").build(),
            SupportedOperation.builder().name("ANY").build(),
            SupportedOperation.builder().name("SOME").build(),
            SupportedOperation.builder().name("ALL").build(),
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
            SupportedOperation.builder().name("BIN").build(),
            SupportedOperation.builder().name("BINARY").build(),
            SupportedOperation.builder().name("CASE").build(),
            SupportedOperation.builder().name("CAST").build(),
            SupportedOperation.builder().name("IF").build(),
            SupportedOperation.builder().name("IFNULL").build(),
            SupportedOperation.builder().name("NULLIF").build(),
            SupportedOperation.builder().name("COALESCE").build(),
            SupportedOperation.builder().name("EXISTS").build(),
            SupportedOperation.builder().name("IS NULL").build(),
            SupportedOperation.builder().name("ISNULL").build(),
            SupportedOperation.builder().name("IS NOT NULL").build(),
            SupportedOperation.builder().name("IS TRUE").build(),
            SupportedOperation.builder().name("IS NOT TRUE").build(),
            SupportedOperation.builder().name("IS FALSE").build(),
            SupportedOperation.builder().name("IS NOT FALSE").build(),

            //String Functions
            SupportedOperation.builder().name("ASCII").build(),
            SupportedOperation.builder().name("CHAR_LENGTH").build(),
            SupportedOperation.builder().name("CHARACTER_LENGTH").build(),
            SupportedOperation.builder().name("CONCAT").build(),
            SupportedOperation.builder().name("CONCAT_WS").build(),
            SupportedOperation.builder().name("FIELD").build(),
            SupportedOperation.builder().name("FIND_IN_SET").build(),
            SupportedOperation.builder().name("FORMAT").build(),
            SupportedOperation.builder().name("INSTR").build(),
            SupportedOperation.builder().name("LCASE").build(),
            SupportedOperation.builder().name("LEFT").build(),
            SupportedOperation.builder().name("LENGTH").build(),
            SupportedOperation.builder().name("LOCATE").build(),
            SupportedOperation.builder().name("LOWER").build(),
            SupportedOperation.builder().name("LPAD").build(),
            SupportedOperation.builder().name("LTRIM").build(),
            SupportedOperation.builder().name("MID").build(),
            SupportedOperation.builder().name("POSITION").build(),
            SupportedOperation.builder().name("REPEAT").build(),
            SupportedOperation.builder().name("REPLACE").build(),
            SupportedOperation.builder().name("REVERSE").build(),
            SupportedOperation.builder().name("RIGHT").build(),
            SupportedOperation.builder().name("RPAD").build(),
            SupportedOperation.builder().name("RTRIM").build(),
            SupportedOperation.builder().name("SPACE").build(),
            SupportedOperation.builder().name("STRCMP").build(),
            SupportedOperation.builder().name("SUBSTR").build(),
            SupportedOperation.builder().name("SUBSTRING").build(),
            SupportedOperation.builder().name("SUBSTRING_INDEX").build(),
            SupportedOperation.builder().name("TRIM").build(),
            SupportedOperation.builder().name("UCASE").build(),
            SupportedOperation.builder().name("UPPER").build(),

            //Math Functions
            SupportedOperation.builder().name("ABS").build(),
            SupportedOperation.builder().name("ACOS").build(),
            SupportedOperation.builder().name("ASIN").build(),
            SupportedOperation.builder().name("ATAN").build(),
            SupportedOperation.builder().name("ATAN2").build(),
            SupportedOperation.builder().name("CEIL").build(),
            SupportedOperation.builder().name("CEILING").build(),
            SupportedOperation.builder().name("COS").build(),
            SupportedOperation.builder().name("COT").build(),
            SupportedOperation.builder().name("DEGREES").build(),
            SupportedOperation.builder().name("DIV").build(),
            SupportedOperation.builder().name("EXP").build(),
            SupportedOperation.builder().name("FLOOR").build(),
            SupportedOperation.builder().name("GREATEST").build(),
            SupportedOperation.builder().name("LEAST").build(),
            SupportedOperation.builder().name("LN").build(),
            SupportedOperation.builder().name("LOG").build(),
            SupportedOperation.builder().name("LOG10").build(),
            SupportedOperation.builder().name("LOG2").build(),
            SupportedOperation.builder().name("MOD").build(),
            SupportedOperation.builder().name("PI").build(),
            SupportedOperation.builder().name("POW").build(),
            SupportedOperation.builder().name("POWER").build(),
            SupportedOperation.builder().name("RADIANS").build(),
            SupportedOperation.builder().name("RAND").build(),
            SupportedOperation.builder().name("ROUND").build(),
            SupportedOperation.builder().name("SIGN").build(),
            SupportedOperation.builder().name("SQRT").build(),
            SupportedOperation.builder().name("TAN").build(),
            SupportedOperation.builder().name("TRUNCATE").build(),

            //Time Functions.
            SupportedOperation.builder().name("ADDDATE").build(),
            SupportedOperation.builder().name("ADDTIME").build(),
            SupportedOperation.builder().name("CURDATE").build(),
            SupportedOperation.builder().name("CURRENT_DATE").build(),
            SupportedOperation.builder().name("CURRENT_TIME").build(),
            SupportedOperation.builder().name("CURRENT_TIMESTAMP").build(),
            SupportedOperation.builder().name("CURTIME").build(),
            SupportedOperation.builder().name("DATE").build(),
            SupportedOperation.builder().name("DATEDIFF").build(),
            SupportedOperation.builder().name("DATE_ADD").build(),
            SupportedOperation.builder().name("DATE_FORMAT").build(),
            SupportedOperation.builder().name("DATE_SUB").build(),
            SupportedOperation.builder().name("DAY").build(),
            SupportedOperation.builder().name("DAYNAME").build(),
            SupportedOperation.builder().name("DAYOFMONTH").build(),
            SupportedOperation.builder().name("DAYOFWEEK").build(),
            SupportedOperation.builder().name("DAYOFYEAR").build(),
            SupportedOperation.builder().name("EXTRACT").build(),
            SupportedOperation.builder().name("FROM_DAYS").build(),
            SupportedOperation.builder().name("HOUR").build(),
            SupportedOperation.builder().name("LAST_DAY").build(),
            SupportedOperation.builder().name("LOCALTIME").build(),
            SupportedOperation.builder().name("LOCALTIMESTAMP").build(),
            SupportedOperation.builder().name("MAKEDATE").build(),
            SupportedOperation.builder().name("MAKETIME").build(),
            SupportedOperation.builder().name("MICROSECOND").build(),
            SupportedOperation.builder().name("MINUTE").build(),
            SupportedOperation.builder().name("MONTH").build(),
            SupportedOperation.builder().name("MONTHNAME").build(),
            SupportedOperation.builder().name("NOW").build(),
            SupportedOperation.builder().name("PERIOD_ADD").build(),
            SupportedOperation.builder().name("PERIOD_DIFF").build(),
            SupportedOperation.builder().name("QUARTER").build(),
            SupportedOperation.builder().name("SECOND").build(),
            SupportedOperation.builder().name("SEC_TO_TIME").build(),
            SupportedOperation.builder().name("STR_TO_DATE").build(),
            SupportedOperation.builder().name("SUBDATE").build(),
            SupportedOperation.builder().name("SUBTIME").build(),
            SupportedOperation.builder().name("SYSDATE").build(),
            SupportedOperation.builder().name("TIME").build(),
            SupportedOperation.builder().name("TIME_FORMAT").build(),
            SupportedOperation.builder().name("TIME_TO_SEC").build(),
            SupportedOperation.builder().name("TIMEDIFF").build(),
            SupportedOperation.builder().name("TIMESTAMP").build(),
            SupportedOperation.builder().name("TO_DAYS").build(),
            SupportedOperation.builder().name("WEEK").build(),
            SupportedOperation.builder().name("WEEKDAY").build(),
            SupportedOperation.builder().name("WEEKOFYEAR").build(),
            SupportedOperation.builder().name("YEAR").build(),
            SupportedOperation.builder().name("YEARWEEK").build(),
            SupportedOperation.builder().name("PARSEDATETIME").build(),
            SupportedOperation.builder().name("FORMATDATETIME").build()
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
    public boolean useASBeforeTableAlias() {
        return true;
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
