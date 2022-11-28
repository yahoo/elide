/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.query.QueryPlanTranslator.addHiddenProjections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

public class PhysicalRefColumnContextTest {

    private SQLTable revenueFactTable;
    private MetaDataStore metaDataStore;
    private Map<String, Argument> queryArgs = new HashMap<>();

    public PhysicalRefColumnContextTest() {
        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(RevenueFact.class));
        models.add(ClassType.of(CurrencyRate.class));

        EntityDictionary dictionary = EntityDictionary.builder().build();

        models.stream().forEach(dictionary::bindEntity);

        metaDataStore = new MetaDataStore(dictionary.getScanner(), models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        // The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, (unused) ->
                new ConnectionDetails(mockDataSource, SQLDialectFactory.getDefaultDialect()));

        revenueFactTable = metaDataStore.getTable(ClassType.of(RevenueFact.class));

        queryArgs.put("format", Argument.builder().name("format").value("999999D000000").build());
        queryArgs.put("testPercentage", Argument.builder().name("testPercentage").value("0.1").build());
    }

    @Test
    public void testLogicalReference() {

        Map<String, Argument> impressionsArg = new HashMap<>();
        impressionsArg.put("aggregation", Argument.builder().name("aggregation").value("SUM").build());
        SQLMetricProjection impressions = (SQLMetricProjection) revenueFactTable.getMetricProjection("impressions",
                        "impressions", impressionsArg);

        Map<String, Argument> testImpressionsArg = new HashMap<>();
        testImpressionsArg.put("aggregation", Argument.builder().name("aggregation").value("MIN").build());
        SQLMetricProjection testImpressions = (SQLMetricProjection) revenueFactTable
                        .getMetricProjection("testImpressions", "testImpressions", testImpressionsArg);

        SQLMetricProjection testRevenue = (SQLMetricProjection) revenueFactTable.getMetricProjection("testRevenue");
        SQLMetricProjection testRevenueLogicalRef = (SQLMetricProjection) revenueFactTable.getMetricProjection("testRevenueLogicalRef");

        Map<String, Argument> revenueArg = new HashMap<>();
        revenueArg.put("format", Argument.builder().name("format").value("11D00").build());
        SQLMetricProjection revenueWithArg = (SQLMetricProjection) revenueFactTable.getMetricProjection("revenue",
                        "revenueWithArg", revenueArg);

        SQLDimensionProjection conversionRate = (SQLDimensionProjection) revenueFactTable.getDimensionProjection("conversionRate");
        SQLDimensionProjection rateProvider = (SQLDimensionProjection) revenueFactTable.getDimensionProjection("rateProvider");

        Query query = Query.builder()
                        .source(revenueFactTable)
                        .metricProjection(impressions)
                        .metricProjection(testImpressions)
                        .metricProjection(testRevenue)
                        .metricProjection(testRevenueLogicalRef)
                        .metricProjection(revenueWithArg)
                        .dimensionProjection(conversionRate)
                        .dimensionProjection(rateProvider)
                        .arguments(queryArgs)
                        .build();

        Query.QueryBuilder builder = Query.builder()
                        .source(query.getSource())
                        .metricProjections(query.getMetricProjections())
                        .dimensionProjections(query.getDimensionProjections())
                        .arguments(query.getArguments());

        Query expandedQuery = addHiddenProjections(metaDataStore, builder, query).build();

        // definition: {{$$column.args.aggregation}}({{$impressions}})
        // -> value of 'aggregation' argument is passed in the query for "impressions" column and same is used while
        // resolving this column.
        assertEquals("SUM({{$impressions}})", impressions.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: {{impressions}}) * {{$$table.args.testPercentage}}
        // -> default value of table argument 'testPercentage' is used.
        // -> value of 'aggregation' argument is passed in the query for "testImpressions" column and same is used while
        // resolving referenced column "impressions".
        assertEquals("MIN({{$impressions}})) * 0.1", testImpressions.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: {{revenue}}
        // revenue definition: TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, {{$$column.args.format}})
        // -> default value of 'format' argument in "revenue" column is used while resolving this column.
        // -> default value of 'format' argument in "revenue" column is passed to joined table's "conversionRate" column.
        assertEquals("TO_CHAR(SUM({{$revenue}}) * TO_CHAR({{rate.$conversion_rate}}, 99D00), 99D00)",
                        testRevenue.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: {{revenueUsingLogicalRef}}
        // revenueUsingLogicalRef's definition: TO_CHAR(SUM({{$revenue}}) * {{conversionRate}}, {{$$column.args.format}})
        // -> default value of 'format' argument in "revenueUsingLogicalRef" column is used while resolving this column.
        // -> This column references "conversionRate" which references "rate.conversionRate". Since conversionRate doesn't have
        // 'format' argument defined, default value of 'format' argument in joined table's "conversionRate" column is used.
        assertEquals("TO_CHAR(SUM({{$revenue}}) * TO_CHAR({{rate.$conversion_rate}}, 9D0), 99D00)",
                        testRevenueLogicalRef.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, {{$$column.args.format}})
        // -> value of 'format' argument is passed in the query for "revenue" column and same is used for resolving
        // referenced column "rate.conversionRate" and this column.
        assertEquals("TO_CHAR(SUM({{$revenue}}) * TO_CHAR({{rate.$conversion_rate}}, 11D00), 11D00)",
                        revenueWithArg.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: {{rate.conversionRate}}
        // -> logical column 'conversionRate' doesn't support arguments.
        // -> default value of 'format' argument in "conversionRate" column of joined table is used while resolving this.
        assertEquals("TO_CHAR({{rate.$conversion_rate}}, 9D0)", conversionRate.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: {{rate.$provider}}
        assertEquals("{{rate.$provider}}", rateProvider.toPhysicalReferences(expandedQuery, metaDataStore));
    }

    @Test
    public void testSqlHelper() {

        Map<String, Argument> revenueArg = new HashMap<>();
        revenueArg.put("format", Argument.builder().name("format").value("11D00").build());
        SQLMetricProjection revenueUsingSqlHelper = (SQLMetricProjection) revenueFactTable
                        .getMetricProjection("revenueUsingSqlHelper", "revenueUsingSqlHelper", revenueArg);

        SQLMetricProjection impressionsPerUSD = (SQLMetricProjection) revenueFactTable.getMetricProjection("impressionsPerUSD");

        Map<String, Argument> impressionsPerUSDArg = new HashMap<>();
        impressionsPerUSDArg.put("format", Argument.builder().name("format").value("11D00").build());
        SQLMetricProjection impressionsPerUSDWithArg = (SQLMetricProjection) revenueFactTable
                        .getMetricProjection("impressionsPerUSD", "impressionsPerUSDWithArg", impressionsPerUSDArg);
        // impressionsPerUSD2 invokes 'revenueUsingSqlHelper' instead of 'revenue'.
        SQLMetricProjection impressionsPerUSD2 =  (SQLMetricProjection) revenueFactTable.getMetricProjection("impressionsPerUSD2");

        Query query = Query.builder()
                        .source(revenueFactTable)
                        .metricProjection(revenueUsingSqlHelper)
                        .metricProjection(impressionsPerUSD)
                        .metricProjection(impressionsPerUSD2)
                        .arguments(queryArgs)
                        .build();

        Query.QueryBuilder builder = Query.builder()
                        .source(query.getSource())
                        .metricProjections(query.getMetricProjections())
                        .arguments(query.getArguments());

        Query expandedQuery = addHiddenProjections(metaDataStore, builder, query).build();

        // definition: TO_CHAR(SUM({{$revenue}}) * {{sql from='rate' column='conversionRate[format:9999D0000]'}}, {{$$column.args.format}})
        // -> value of 'format' argument is passed in the query for "revenueUsingSqlHelper" column and same is used for
        // resolving this column.
        // -> pinned value (9999D0000) of 'format' argument in SQL helper is used while resolving referenced column "rate.conversionRate".
        assertEquals("TO_CHAR(SUM({{$revenue}}) * TO_CHAR({{rate.$conversion_rate}}, 9999D0000), 11D00)",
                        revenueUsingSqlHelper.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: TO_CHAR({{sql column='impressions[aggregation:SUM]'}} / {{sql column='revenue[format:99999D00000]'}}, {{$$table.args.format}})
        // -> {{$$table.args.format}} is resolved using query argument 'format' (999999D000000).
        // -> pinned value (SUM) of 'aggregation' argument in SQL helper is used while resolving invoked column "impressions".
        // -> pinned value (99999D00000) of 'format' argument in SQL helper is used while resolving invoked column "revenue".
        // -> revenue definition is : TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, {{$$column.args.format}}),
        // Available value of 'format' argument in "revenue" column is passed to joined table's "conversionRate" column.
        assertEquals("TO_CHAR(SUM({{$impressions}}) / TO_CHAR(SUM({{$revenue}}) * TO_CHAR({{rate.$conversion_rate}}, 99999D00000), 99999D00000), 999999D000000)",
                        impressionsPerUSD.toPhysicalReferences(expandedQuery, metaDataStore));

        // -> Even 'format' is passed in query column args, pinned value (9999D0000) of 'format' argument in SQL helper is used while
        // resolving "revenue" column and same is passed to joined table's "conversionRate" column.
        assertEquals("TO_CHAR(SUM({{$impressions}}) / TO_CHAR(SUM({{$revenue}}) * TO_CHAR({{rate.$conversion_rate}}, 99999D00000), 99999D00000), 999999D000000)",
                        impressionsPerUSDWithArg.toPhysicalReferences(expandedQuery, metaDataStore));

        // definition: TO_CHAR({{sql column='impressions[aggregation:SUM]'}} / {{sql column='revenueUsingSqlHelper[format:99999D00000]'}}, {{$$table.args.format}})
        // -> As "rate.conversionRate" is invoked using SQL helper from "revenue" column, this uses the fixed value(9999D0000) of
        // 'format' argument provided in definition of "revenueUsingSqlHelper" column.
        assertEquals("TO_CHAR(SUM({{$impressions}}) / TO_CHAR(SUM({{$revenue}}) * TO_CHAR({{rate.$conversion_rate}}, 9999D0000), 99999D00000), 999999D000000)",
                        impressionsPerUSD2.toPhysicalReferences(expandedQuery, metaDataStore));
    }
}
