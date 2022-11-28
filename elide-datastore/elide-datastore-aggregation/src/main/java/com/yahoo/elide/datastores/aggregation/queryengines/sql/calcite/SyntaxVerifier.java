/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
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
import org.apache.calcite.sql.util.SqlBasicVisitor;

import lombok.Getter;

/**
 * Verifies whether Elide can parse a SQL expression with Calcite AND that
 * all operators and aggregation functions are fully supported by the given dialect.
 */
public class SyntaxVerifier extends SqlBasicVisitor<Boolean> {
    private SQLDialect dialect;

    @Getter
    private String lastError;

    public SyntaxVerifier(SQLDialect dialect) {
        this.dialect = dialect;
    }

    public boolean verify(String expression) {
        SqlParser sqlParser = SqlParser.create(expression, CalciteUtils.constructParserConfig(dialect));

        SqlNode node;
        try {
            node = sqlParser.parseExpression();
        } catch (SqlParseException e) {
            lastError = e.getMessage();
            return false;
        }

        return node.accept(this);
    }

    @Override
    public Boolean visit(SqlLiteral literal) {
        return true;
    }

    @Override
    public Boolean visit(SqlCall call) {
        String operatorName = call.getOperator().getName();

        SupportedAggregation aggregation = dialect.getSupportedAggregation(operatorName);

        if (aggregation == null) {
            SupportedOperation operator = dialect.getSupportedOperation(operatorName);

            //We don't understand the operator so we can't nest safely.
            if (operator == null) {
                lastError = "Unknown operator: " + operatorName;
                return false;
            }
        }

        boolean result = true;
        for (int idx = 0; idx < call.getOperandList().size(); idx++) {
            SqlNode operand = call.getOperandList().get(idx);
            if (operand == null) {
                continue;
            }
            result &= operand.accept(this);
        }
        return result;
    }

    @Override
    public Boolean visit(SqlNodeList nodeList) {
        return true;
    }

    @Override
    public Boolean visit(SqlIdentifier id) {
        return true;
    }

    @Override
    public Boolean visit(SqlDataTypeSpec type) {
        return true;
    }

    @Override
    public Boolean visit(SqlDynamicParam param) {
        return true;
    }

    @Override
    public Boolean visit(SqlIntervalQualifier intervalQualifier) {
        return true;
    }
}
