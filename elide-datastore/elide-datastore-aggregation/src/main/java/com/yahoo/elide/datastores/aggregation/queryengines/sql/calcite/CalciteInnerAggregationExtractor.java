/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CalciteInnerAggregationExtractor extends SqlBasicVisitor<List<String>> {

    private SQLDialect dialect;

    public CalciteInnerAggregationExtractor(SQLDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public List<String> visit(SqlCall call) {
        String operatorName = call.getOperator().getName();

        List<String> result = new ArrayList<>();

        SupportedAggregation operator = dialect.getSupportedAggregation(operatorName);
        if (operator != null) {
            List<String> operands = call.getOperandList().stream()
                    .map(operand -> operand.toSqlString(dialect.getCalciteDialect()).getSql())
                    .collect(Collectors.toList());

            return operator.getInnerAggregations(operands.toArray(new String[0]));
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
