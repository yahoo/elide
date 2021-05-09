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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

public class LogicalRefContextTest {

    private SQLTable revenueFactTable;
    private MetaDataStore metaDataStore;
    private Map<String, Argument> queryArgs = new HashMap<>();
    private SQLReferenceTable refTable;

    public LogicalRefContextTest() {
        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(RevenueFact.class));
        models.add(ClassType.of(CurrencyRate.class));

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        models.stream().forEach(dictionary::bindEntity);

        metaDataStore = new MetaDataStore(models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        // The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, new ConnectionDetails(mockDataSource, SQLDialectFactory.getDefaultDialect()));

        refTable = new SQLReferenceTable(metaDataStore);
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

        Query expandedQuery = addHiddenProjections(refTable, builder, query).build();

        // definition: {{$$column.args.aggregation}}({{$impressions}})
        // -> value of 'aggregation' argument is passed in the query for "impressions" column and same is used while
        // resolving this column.
        assertEquals("SUM({{$impressions}})", impressions.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: {{impressions}}) * {{$$table.args.testPercentage}}
        // -> default value of table argument 'testPercentage' is used.
        // -> value of 'aggregation' argument is passed in the query for "testImpressions" column and same is used while
        // resolving referenced column "impressions".
        assertEquals("MIN({{$impressions}})) * 0.1", testImpressions.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: {{revenue}}
        // revenue definition: TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, {{$$column.args.format}})
        // -> default value of 'format' argument in "revenue" column is used while resolving this column.
        assertEquals("TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, 99D00)",
                        testRevenue.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: {{revenueUsingLogicalRef}}
        // revenueUsingLogicalRef's definition: TO_CHAR(SUM({{$revenue}}) * {{conversionRate}}, {{$$column.args.format}})
        // -> default value of 'format' argument in "revenue" column is used while resolving this column.
        assertEquals("TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, 99D00)",
                        testRevenueLogicalRef.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, {{$$column.args.format}})
        // -> value of 'format' argument is passed in the query for "revenue" column and same is used for resolving
        // this column.
        assertEquals("TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, 11D00)",
                        revenueWithArg.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: {{rate.conversionRate}}
        assertEquals("{{rate.conversionRate}}", conversionRate.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: {{rate.$provider}}
        assertEquals("{{rate.$provider}}", rateProvider.resolveLogicalReferences(expandedQuery, metaDataStore));
    }

    @Test
    public void testSqlHelper() {

        Map<String, Argument> revenueArg = new HashMap<>();
        revenueArg.put("format", Argument.builder().name("format").value("11D00").build());
        SQLMetricProjection revenueUsingSqlHelper = (SQLMetricProjection) revenueFactTable
                        .getMetricProjection("revenueUsingSqlHelper", "revenueUsingSqlHelper", revenueArg);

        SQLMetricProjection impressionsPerUSD = (SQLMetricProjection) revenueFactTable.getMetricProjection("impressionsPerUSD");
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

        Query expandedQuery = addHiddenProjections(refTable, builder, query).build();

        // definition: TO_CHAR(SUM({{$revenue}}) * {{sql from='rate' column='conversionRate[format:9999D0000]'}}, {{$$column.args.format}})
        // -> value of 'format' argument is passed in the query for "revenueUsingSqlHelper" column and same is used for
        // resolving this column.
        assertEquals("TO_CHAR(SUM({{$revenue}}) * {{sql from='rate' column='conversionRate[format:9999D0000]'}}, 11D00)",
                        revenueUsingSqlHelper.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: TO_CHAR({{sql column='impressions[aggregation:SUM]'}} / {{sql column='revenue[format:99999D00000]'}}, {{$$table.args.format}})
        // -> {{$$table.args.format}} is resolved using query argument 'format' (999999D000000).
        // -> pinned value (SUM) of 'aggregation' argument in SQL helper is used while resolving invoked column "impressions".
        // -> pinned value (99999D00000) of 'format' argument in SQL helper is used while resolving invoked column "revenue".
        // -> revenue definition is : TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, {{$$column.args.format}})
        assertEquals("TO_CHAR(SUM({{$impressions}}) / TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, 99999D00000), 999999D000000)",
                        impressionsPerUSD.resolveLogicalReferences(expandedQuery, metaDataStore));

        // definition: TO_CHAR({{sql column='impressions[aggregation:SUM]'}} / {{sql column='revenueUsingSqlHelper[format:99999D00000]'}}, {{$$table.args.format}})
        assertEquals("TO_CHAR(SUM({{$impressions}}) / TO_CHAR(SUM({{$revenue}}) * {{sql from='rate' column='conversionRate[format:9999D0000]'}}, 99999D00000), 999999D000000)",
                        impressionsPerUSD2.resolveLogicalReferences(expandedQuery, metaDataStore));
    }
}
