/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import example.PlayerStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
public class EntityProjectionTranslatorTest extends SQLUnitTest {
    private static EntityProjection basicProjection = EntityProjection.builder()
            .type(PlayerStats.class)
            .attribute(Attribute.builder()
                    .type(long.class)
                    .name("lowScore")
                    .argument(Argument.builder().name("foo").value("bar").build())
                    .build())
            .attribute(Attribute.builder()
                    .type(String.class)
                    .name("overallRating")
                    .build())
            .argument(Argument.builder().name("foo").value("bar").build())
            .build();

    @Mock private RequestScope scope;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
    }

    @Test
    public void testBasicTranslation() {
        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                engine,
                playerStatsTable,
                basicProjection,
                scope,
                true
        );

        Query query = translator.getQuery();

        assertEquals(playerStatsTable, query.getSource());
        assertEquals(1, query.getMetricProjections().size());
        String actual = query.getMetricProjections().stream()
                .map(MetricProjection::getAlias)
                .findFirst()
                .orElse(null);

        assertEquals("lowScore", actual);
        assertEquals(1, query.getAllDimensionProjections().size());

        List<ColumnProjection> dimensions = new ArrayList<>(query.getAllDimensionProjections());
        assertEquals("overallRating", dimensions.get(0).getName());
    }

    @Test
    public void testWherePromotion() throws ParseException {
        FilterExpression originalFilter = filterParser.parseFilterExpression("overallRating==Good,lowScore[foo:bar]<45",
                playerStatsType, false);

        EntityProjection projection = basicProjection.copyOf()
                .filterExpression(originalFilter)
                .build();

        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                engine,
                playerStatsTable,
                projection,
                scope,
                true
        );

        Query query = translator.getQuery();

        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(playerStatsTable);
        FilterConstraints constraints = originalFilter.accept(visitor);
        FilterExpression whereFilter = constraints.getWhereExpression();
        FilterExpression havingFilter = constraints.getHavingExpression();
        assertEquals(whereFilter, query.getWhereFilter());
        assertEquals(havingFilter, query.getHavingFilter());
    }

    @Test
    public void testTimeDimension() {
        EntityProjection projection = basicProjection.copyOf()
                .attribute(Attribute.builder()
                        .type(Date.class)
                        .name("recordedDate")
                        .build())
                .build();

        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                engine,
                playerStatsTable,
                projection,
                scope,
                true
        );

        Query query = translator.getQuery();

        List<TimeDimensionProjection> timeDimensions = new ArrayList<>(query.getTimeDimensionProjections());
        assertEquals(1, timeDimensions.size());
        assertEquals("recordedDate", timeDimensions.get(0).getAlias());
        assertEquals(TimeGrain.DAY, timeDimensions.get(0).getGrain());
    }

    @Test
    public void testHavingClauseMetricsMissingFromProjection() throws ParseException {
        FilterExpression filter = filterParser.parseFilterExpression("lowScore>45",
                playerStatsType, false);

        EntityProjection projection = EntityProjection.builder()
                .type(PlayerStats.class)
                .filterExpression(filter)
                .attribute(Attribute.builder()
                        .type(long.class)
                        .name("highScore")
                        .build())
                .attribute(Attribute.builder()
                        .type(String.class)
                        .name("overallRating")
                        .build())
                .build();

        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                engine,
                playerStatsTable,
                projection,
                scope,
                true
        );

        Query query = translator.getQuery();

        List<String> metricNames = query.getMetricProjections().stream()
                .map(MetricProjection::getName)
                .collect(Collectors.toList());

        assertEquals(metricNames, Arrays.asList("highScore", "lowScore"));
    }

    @Test
    public void testQueryArguments() {
        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                engine,
                playerStatsTable,
                basicProjection,
                scope,
                true
        );

        Query query = translator.getQuery();
        Argument queryArg = query.getArguments().get("foo");
        assertEquals("bar", queryArg.getValue());

        Argument lowScoreArg = query.getColumnProjection("lowScore").getArguments().get("foo");
        assertEquals("bar", lowScoreArg.getValue());
    }
}
