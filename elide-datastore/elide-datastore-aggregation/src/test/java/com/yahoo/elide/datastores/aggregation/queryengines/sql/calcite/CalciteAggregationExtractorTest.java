/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CalciteAggregationExtractorTest {

    @Test
    public void testExpressionParsing() throws Exception {
        String sql = "        SUM (CASE\n" +
                "                WHEN 'number_of_lectures' > 20 then 1\n" +
                "                ELSE 0\n" +
                "        END) / SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config());
        SqlNode node = sqlParser.parseExpression();
        CalciteAggregationExtractor extractor = new CalciteAggregationExtractor();
        List<String> aggregations = node.accept(extractor);

        assertEquals(2, aggregations.size());
        assertEquals("SUM(CASE WHEN 'number_of_lectures' > 20 THEN 1 ELSE 0 END)", aggregations.get(0));
        assertEquals("SUM(BLAH)", aggregations.get(1));
        System.out.println(aggregations);
    }

    @Test
    public void testInvalidAggregationFunction() throws Exception {
        String sql = "CUSTOM_SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config());
        SqlNode node = sqlParser.parseExpression();
        CalciteAggregationExtractor extractor = new CalciteAggregationExtractor();
        List<String> aggregations = node.accept(extractor);

        assertTrue(aggregations.isEmpty());
    }

    @Test
    public void testCustomAggregationFunction() throws Exception {
        String sql = "CUSTOM_SUM(blah)";
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config());
        SqlNode node = sqlParser.parseExpression();
        CalciteAggregationExtractor extractor =
                new CalciteAggregationExtractor(new HashSet<>(Arrays.asList("CUSTOM_SUM")));
        List<String> aggregations = node.accept(extractor);

        assertEquals(1, aggregations.size());
        assertEquals("CUSTOM_SUM(BLAH)", aggregations.get(0));
    }
}
