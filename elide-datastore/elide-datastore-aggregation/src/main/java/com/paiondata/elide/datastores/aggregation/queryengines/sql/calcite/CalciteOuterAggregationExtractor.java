/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlDynamicParam;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Parses a column expression and rewrites the post aggregation expression AST to reference
 * new aliases defined in the inner query.  Aggregation functions that cannot be nested (like AVG(expression) are
 * rewritten with simpler aggregations that can be: SUM(INNER_LABEL) / COUNT(INNER_LABEL).
 */
public class CalciteOuterAggregationExtractor extends SqlBasicVisitor<SqlNode> {

    private SQLDialect dialect;
    private Queue<List<String>> substitutions;

    public CalciteOuterAggregationExtractor(SQLDialect dialect, List<List<String>> substitutions) {
        this.dialect = dialect;
        this.substitutions = new ArrayDeque<>();
        this.substitutions.addAll(substitutions);
    }

    @Override
    public SqlNode visit(SqlCall call) {
        String operatorName = call.getOperator().getName();

        SupportedAggregation operator = dialect.getSupportedAggregation(operatorName);
        if (operator != null) {
            List<String> expressionsSubs = substitutions.remove();
            String postAggExpression = operator.getOuterAggregation(expressionsSubs.toArray(new String[0]));

            SqlParser sqlParser = SqlParser.create(postAggExpression, CalciteUtils.constructParserConfig(dialect));

            try {
                return sqlParser.parseExpression();
            } catch (SqlParseException e) {
                throw new IllegalStateException(e);
            }
        }
        for (int idx = 0; idx < call.getOperandList().size(); idx++) {
            SqlNode operand = call.getOperandList().get(idx);
            call.setOperand(idx, operand == null ? null : operand.accept(this));
        }

        return call;
    }

    @Override
    public SqlNode visit(SqlNodeList nodeList) {

        return SqlNodeList.of(SqlParserPos.ZERO,
                nodeList.getList().stream()
                    .map(sqlNode -> sqlNode == null ? null : sqlNode.accept(this))
                    .collect(Collectors.toList()));
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
