/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalciteAggregationExtractor extends SqlBasicVisitor<List<String>> {

    private SqlDialect dialect;
    private Set<String> customAggregationFunctions;

    public CalciteAggregationExtractor() {
        this(new SqlDialect(SqlDialect.EMPTY_CONTEXT
                .withCaseSensitive(true)
                .withQuotedCasing(Casing.UNCHANGED)
                .withUnquotedCasing(Casing.UNCHANGED)), new HashSet<>());
    }

    public CalciteAggregationExtractor(Set<String> customAggregationFunctions) {
        this(new SqlDialect(SqlDialect.EMPTY_CONTEXT
                .withCaseSensitive(true)
                .withQuotedCasing(Casing.UNCHANGED)
                .withUnquotedCasing(Casing.UNCHANGED)), customAggregationFunctions);
    }

    public CalciteAggregationExtractor(SqlDialect dialect, Set<String> customAggregationFunctions) {
        this.dialect = dialect;
        this.customAggregationFunctions = customAggregationFunctions;
    }

    @Override
    public List<String> visit(SqlCall call) {
        String operatorName = call.getOperator().getName();

        List<String> result = new ArrayList<>();
        StandardAggregations operator = StandardAggregations.find(operatorName);
        if (operator != null || customAggregationFunctions.contains(operatorName)) {
            result.add(call.toSqlString(dialect).getSql());
            return result;
        }

        for (SqlNode node : call.getOperandList()) {
            List<String> operandResults = node.accept(this);
            if (operandResults != null) {
                result.addAll(operandResults);
            }
        }
        return result;
    }

    @Override
    public List<String> visit(SqlNodeList nodeList) {
        List<String> result = new ArrayList<>();
        for (SqlNode node : nodeList) {
            List<String> inner = node.accept(this);
            if (inner != null) {
                result.addAll(inner);
            }
        }
        return result;
    }
}
