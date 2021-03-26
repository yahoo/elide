/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CalciteOuterAggregationExtractorTest {

    private SqlDialect dialect = new SqlDialect(SqlDialect.EMPTY_CONTEXT
            .withCaseSensitive(true)
            .withQuotedCasing(Casing.UNCHANGED)
            .withUnquotedCasing(Casing.UNCHANGED));

    @Test
    public void testExpressionParsing() throws Exception {
        String sql = "        SUM (CASE\n"
                + "                WHEN 'number_of_lectures' > 20 then 1\n"
                + "                ELSE 0\n"
                + "        END) / SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config());
        SqlNode node = sqlParser.parseExpression();

        List<String> substitutions = Arrays.asList("SUB1", "SUB2");
        CalciteOuterAggregationExtractor extractor = new CalciteOuterAggregationExtractor(substitutions.iterator());
        String actual = node.accept(extractor).toSqlString(dialect).getSql();
        String expected = "SUM(SUB1) / SUM(SUB2)";

        assertEquals(expected, actual);
    }

    @Test
    public void testCustomAggregationFunction() throws Exception {
        String sql = "CUSTOM_SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config());
        SqlNode node = sqlParser.parseExpression();

        List<String> substitutions = Arrays.asList("SUB1");
        CalciteOuterAggregationExtractor extractor = new CalciteOuterAggregationExtractor(substitutions.iterator(),
                new HashSet<>(Arrays.asList("CUSTOM_SUM")));
        String actual = node.accept(extractor).toSqlString(dialect).getSql();
        String expected = "CUSTOM_SUM(SUB1)";

        assertEquals(expected, actual);
    }
}
