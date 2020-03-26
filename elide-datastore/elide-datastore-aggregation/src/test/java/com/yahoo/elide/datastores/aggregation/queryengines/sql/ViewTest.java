/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.request.Sorting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ViewTest extends SQLUnitTest {
    protected static Table playerStatsWithViewSchema;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
        playerStatsWithViewSchema = engine.getTable("playerStatsWithView");
    }

    @Test
    public void testViewAttribute() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsWithViewSchema)
                .metric(invoke(playerStatsWithViewSchema.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsWithViewSchema.getDimension("countryViewIsoCode")))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setCountryViewIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testNestedViewAttribute() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsWithViewSchema)
                .metric(invoke(playerStatsWithViewSchema.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsWithViewSchema.getDimension("countryViewViewIsoCode")))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setCountryViewViewIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewViewIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testNestedRelationshipAttribute() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsWithViewSchema)
                .metric(invoke(playerStatsWithViewSchema.getMetric("lowScore")))
                .groupByDimension(
                        toProjection(playerStatsWithViewSchema.getDimension("countryViewViewIsoCode")))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setCountryViewViewIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewViewIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testSortingViewAttribute() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsWithViewSchema)
                .metric(invoke(playerStatsWithViewSchema.getMetric("lowScore")))
                .groupByDimension(
                        toProjection(playerStatsWithViewSchema.getDimension("countryViewViewIsoCode")))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setCountryViewViewIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewViewIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testSortingNestedViewAttribute() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsWithViewSchema)
                .metric(invoke(playerStatsWithViewSchema.getMetric("lowScore")))
                .groupByDimension(
                        toProjection(playerStatsWithViewSchema.getDimension("countryViewViewIsoCode")))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setCountryViewViewIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewViewIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testSortingNestedRelationshipAttribute() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsWithViewSchema)
                .metric(invoke(playerStatsWithViewSchema.getMetric("lowScore")))
                .groupByDimension(
                        toProjection(playerStatsWithViewSchema.getDimension("countryViewViewIsoCode")))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setCountryViewViewIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewViewIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }
}
