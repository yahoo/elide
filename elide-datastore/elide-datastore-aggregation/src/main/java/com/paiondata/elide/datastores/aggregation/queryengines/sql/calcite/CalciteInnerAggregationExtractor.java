/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses a column expression and extracts all aggregation functions as separate expressions for
 * an inner query.  Aggregation functions that cannot be nested (like AVG(expression) are rewritten
 * with simpler aggregations that can be: [SUM(expression), COUNT(expression)].  The list of
 * inner query expressions will be leveraged in the outer query post aggregation expression.
 */
public class CalciteInnerAggregationExtractor extends SqlBasicVisitor<List<List<String>>> {

    private SQLDialect dialect;

    public CalciteInnerAggregationExtractor(SQLDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public List<List<String>> visit(SqlCall call) {
        String operatorName = call.getOperator().getName();

        List<List<String>> result = new ArrayList<>();

        SupportedAggregation operator = dialect.getSupportedAggregation(operatorName);
        if (operator != null) {
            List<String> operands = call.getOperandList().stream()
                    .map(operand -> operand.toSqlString(dialect.getCalciteDialect()).getSql())
                    .collect(Collectors.toList());

            result.add(operator.getInnerAggregations(operands.toArray(new String[0])));

            return result;
        }

        for (SqlNode node : call.getOperandList()) {
            if (node == null) {
                continue;
            }
            List<List<String>> operandResults = node.accept(this);
            if (operandResults != null) {
                result.addAll(operandResults);
            }
        }
        return result;
    }

    @Override
    public List<List<String>> visit(SqlNodeList nodeList) {
        List<List<String>> result = new ArrayList<>();
        for (SqlNode node : nodeList) {
            if (node == null) {
                continue;
            }
            List<List<String>> inner = node.accept(this);
            if (inner != null) {
                result.addAll(inner);
            }
        }
        return result;
    }
}
