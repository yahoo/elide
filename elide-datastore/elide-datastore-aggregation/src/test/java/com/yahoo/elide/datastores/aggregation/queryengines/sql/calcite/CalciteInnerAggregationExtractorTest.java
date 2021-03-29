/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CalciteInnerAggregationExtractorTest {

    private SQLDialect dialect = new H2Dialect();

    @Test
    public void testExpressionParsing() throws Exception {
        String sql = "        SUM (CASE\n"
                + "                WHEN `number_of_lectures` > 20 then 1\n"
                + "                ELSE 0\n"
                + "        END) / SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config().withLex(dialect.getCalciteLex()));
        SqlNode node = sqlParser.parseExpression();
        CalciteInnerAggregationExtractor extractor = new CalciteInnerAggregationExtractor(dialect);
        List<String> aggregations = node.accept(extractor);

        assertEquals(2, aggregations.size());
        assertEquals("SUM(CASE WHEN `number_of_lectures` > 20 THEN 1 ELSE 0 END)", aggregations.get(0));
        assertEquals("SUM(`blah`)", aggregations.get(1));
    }

    @Test
    public void testInvalidAggregationFunction() throws Exception {
        String sql = "CUSTOM_SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config().withLex(dialect.getCalciteLex()));
        SqlNode node = sqlParser.parseExpression();
        CalciteInnerAggregationExtractor extractor = new CalciteInnerAggregationExtractor(dialect);
        List<String> aggregations = node.accept(extractor);

        assertTrue(aggregations.isEmpty());
    }

    @Test
    public void testAverageFunction() throws Exception {
        String sql = "AVG(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config().withLex(dialect.getCalciteLex()));
        SqlNode node = sqlParser.parseExpression();
        CalciteInnerAggregationExtractor extractor = new CalciteInnerAggregationExtractor(dialect);
        List<String> aggregations = node.accept(extractor);

        assertEquals(2, aggregations.size());
        assertEquals("SUM(`blah`)", aggregations.get(0));
        assertEquals("COUNT(`blah`)", aggregations.get(1));
    }
}
