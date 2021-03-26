/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlDynamicParam;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CalciteOuterAggregationExtractor extends SqlBasicVisitor<SqlNode> {

    private SqlDialect dialect;
    private Set<String> customAggregationFunctions;
    private Iterator<String> substitutions;

    public CalciteOuterAggregationExtractor(Iterator<String> substitutions) {
        this(substitutions, new HashSet<>());
    }

    public CalciteOuterAggregationExtractor(Iterator<String> substitutions, Set<String> customAggregationFunctions) {
        this.customAggregationFunctions = customAggregationFunctions;
        this.substitutions = substitutions;
    }

    @Override
    public SqlNode visit(SqlCall call) {
        String operatorName = call.getOperator().getName();

        StandardAggregations operator = StandardAggregations.find(operatorName);
        if (operator != null || customAggregationFunctions.contains(operatorName)) {
            for (int idx = 0; idx < call.getOperandList().size(); idx++) {
                SqlNode operand = call.getOperandList().get(idx);
                if (!substitutions.hasNext()) {
                    throw new IllegalStateException("Expecting more substitutions for outer aggregation expansion");
                }
                String nextSubstitution = substitutions.next();
                call.setOperand(idx, new SqlIdentifier(nextSubstitution,
                        new SqlParserPos(
                                operand.getParserPosition().getLineNum(),
                                operand.getParserPosition().getColumnNum(),
                                operand.getParserPosition().getLineNum(),
                                operand.getParserPosition().getColumnNum() + nextSubstitution.length() - 1
                        )));
            }
        } else {
            for (int idx = 0; idx < call.getOperandList().size(); idx++) {
                SqlNode operand = call.getOperandList().get(idx);
                call.setOperand(idx, operand.accept(this));
            }
        }

        return call;
    }

    @Override
    public SqlNode visit(SqlNodeList nodeList) {
        return nodeList;
    }

    @Override
    public SqlNode visit(SqlLiteral literal) {
        return literal;
    }

    @Override
    public SqlNode visit(SqlIdentifier id) {
        return id;
    }

    @Override
    public SqlNode visit(SqlDataTypeSpec type) {
        return type;
    }

    @Override
    public SqlNode visit(SqlDynamicParam param) {
        return param;
    }

    @Override
    public SqlNode visit(SqlIntervalQualifier intervalQualifier) {
        return intervalQualifier;
    }
}
