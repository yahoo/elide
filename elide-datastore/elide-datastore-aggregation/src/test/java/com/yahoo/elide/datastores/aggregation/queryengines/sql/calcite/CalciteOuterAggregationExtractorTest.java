/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class CalciteOuterAggregationExtractorTest {

    private SQLDialect dialect = new H2Dialect();

    @Test
    public void testExpressionParsing() throws Exception {
        String sql = "        SUM (CASE\n"
                + "                WHEN 'number_of_lectures' > 20 then 1\n"
                + "                ELSE 0\n"
                + "        END) / SUM(blah)";

        SqlParser sqlParser = SqlParser.create(sql, CalciteUtils.constructParserConfig(dialect));
        SqlNode node = sqlParser.parseExpression();

        List<List<String>> substitutions = Arrays.asList(Arrays.asList("SUB1"), Arrays.asList("SUB2"));
        CalciteOuterAggregationExtractor extractor = new CalciteOuterAggregationExtractor(dialect, substitutions);
        String actual = node.accept(extractor).toSqlString(dialect.getCalciteDialect()).getSql();
        String expected = "SUM(`SUB1`) / SUM(`SUB2`)";

        assertEquals(expected, actual);
    }

    @Test
    public void testCaseStmtParsing() throws Exception {
        String sql = "        CASE\n"
                + "                WHEN SUM(blah) = 0 THEN 1\n"
                + "                ELSE SUM('number_of_lectures') / SUM(blah)\n"
                + "        END";

        SqlParser sqlParser = SqlParser.create(sql, CalciteUtils.constructParserConfig(dialect));
        SqlNode node = sqlParser.parseExpression();

        List<List<String>> substitutions = Arrays.asList(Arrays.asList("SUB1"), Arrays.asList("SUB2"), Arrays.asList("SUB1"));
        CalciteOuterAggregationExtractor extractor = new CalciteOuterAggregationExtractor(dialect, substitutions);
        String actual = node.accept(extractor).toSqlString(dialect.getCalciteDialect()).getSql();
        String expected = "CASE WHEN SUM(`SUB1`) = 0 THEN 1 ELSE SUM(`SUB2`) / SUM(`SUB1`) END";

        assertEquals(expected, actual);
    }

    @Test
    public void testCustomAggregationFunction() throws Exception {
        String sql = "CUSTOM_SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, CalciteUtils.constructParserConfig(dialect));
        SqlNode node = sqlParser.parseExpression();

        List<List<String>> substitutions = Arrays.asList(Arrays.asList("SUB1"));
        CalciteOuterAggregationExtractor extractor = new CalciteOuterAggregationExtractor(dialect, substitutions);
        String actual = node.accept(extractor).toSqlString(dialect.getCalciteDialect()).getSql();
        String expected = "`CUSTOM_SUM`(`blah`)";

        assertEquals(expected, actual);
    }

    @Test
    public void testAverageFunction() throws Exception {
        String sql = "AVG(blah)";
        SqlParser sqlParser = SqlParser.create(sql, CalciteUtils.constructParserConfig(dialect));
        SqlNode node = sqlParser.parseExpression();

        List<List<String>> substitutions = Arrays.asList(Arrays.asList("SUB1"));
        CalciteOuterAggregationExtractor extractor = new CalciteOuterAggregationExtractor(dialect, substitutions);
        String actual = node.accept(extractor).toSqlString(dialect.getCalciteDialect()).getSql();
        String expected = "SUM(`SUB1`) / COUNT(`SUB1`)";

        assertEquals(expected, actual);
    }

    @Test
    public void testVarPopFunction() throws Exception {
        String sql = "VAR_POP(blah)";
        SqlParser sqlParser = SqlParser.create(sql, CalciteUtils.constructParserConfig(dialect));
        SqlNode node = sqlParser.parseExpression();

        List<List<String>> substitutions = Arrays.asList(Arrays.asList("SUB1", "SUB2", "SUB3"));

        CalciteOuterAggregationExtractor extractor = new CalciteOuterAggregationExtractor(dialect, substitutions);
        String actual = node.accept(extractor).toSqlString(dialect.getCalciteDialect()).getSql();
        String expected = "(SUM(`SUB1`) - SUM(`SUB2`) * SUM(`SUB2`) / COUNT(`SUB3`)) / COUNT(`SUB3`)";

        assertEquals(expected, actual);
    }
}
