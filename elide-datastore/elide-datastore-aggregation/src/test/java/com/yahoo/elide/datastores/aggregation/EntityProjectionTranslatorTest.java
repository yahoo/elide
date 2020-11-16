/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class EntityProjectionTranslatorTest extends SQLUnitTest {
    private static EntityProjection basicProjection = EntityProjection.builder()
            .type(PlayerStats.class)
            .attribute(Attribute.builder()
                    .type(long.class)
                    .name("lowScore")
                    .build())
            .attribute(Attribute.builder()
                    .type(String.class)
                    .name("overallRating")
                    .build())
            .build();
    protected static MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<String, String>();
    protected static Optional<MultivaluedMap<String, String>> requestHeaderOptional = Optional.ofNullable(requestHeaders);

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
        requestHeaders.add("bypassCache", "true");
    }

    @Test
    public void testBasicTranslation() {
        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                engine,
                playerStatsTable,
                basicProjection,
                dictionary,
                requestHeaderOptional
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
        FilterExpression originalFilter = filterParser.parseFilterExpression("overallRating==Good,lowScore<45",
                PlayerStats.class, false);

        EntityProjection projection = basicProjection.copyOf()
                .filterExpression(originalFilter)
                .build();

        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                engine,
                playerStatsTable,
                projection,
                dictionary,
                requestHeaderOptional
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
                dictionary,
                requestHeaderOptional
        );

        Query query = translator.getQuery();

        List<TimeDimensionProjection> timeDimensions = new ArrayList<>(query.getTimeDimensionProjections());
        assertEquals(1, timeDimensions.size());
        assertEquals("recordedDate", timeDimensions.get(0).getAlias());
        assertEquals(TimeGrain.DAY, timeDimensions.get(0).getGrain());
    }
}
