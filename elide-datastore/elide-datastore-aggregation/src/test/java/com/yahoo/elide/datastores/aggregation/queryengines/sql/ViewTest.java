/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import example.PlayerStatsWithView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ViewTest extends SQLUnitTest {
    protected static SQLTable playerStatsWithViewSchema;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
        playerStatsWithViewSchema =
                (SQLTable) engine.getMetaDataStore().getTable("playerStatsWithView", NO_VERSION);
    }

    @Test
    public void testViewAttribute() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(playerStatsWithViewSchema)
                .metricProjection(playerStatsWithViewSchema.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsWithViewSchema.getDimensionProjection("countryViewIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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
                .source(playerStatsWithViewSchema)
                .metricProjection(playerStatsWithViewSchema.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsWithViewSchema.getDimensionProjection("countryViewViewIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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
                .source(playerStatsWithViewSchema)
                .metricProjection(playerStatsWithViewSchema.getMetricProjection("lowScore"))
                .dimensionProjection(
                        playerStatsWithViewSchema.getDimensionProjection("countryViewViewIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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
                .source(playerStatsWithViewSchema)
                .metricProjection(playerStatsWithViewSchema.getMetricProjection("lowScore"))
                .dimensionProjection(
                        playerStatsWithViewSchema.getDimensionProjection("countryViewViewIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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
                .source(playerStatsWithViewSchema)
                .metricProjection(playerStatsWithViewSchema.getMetricProjection("lowScore"))
                .dimensionProjection(
                        playerStatsWithViewSchema.getDimensionProjection("countryViewViewIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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
                .source(playerStatsWithViewSchema)
                .metricProjection(playerStatsWithViewSchema.getMetricProjection("lowScore"))
                .dimensionProjection(
                        playerStatsWithViewSchema.getDimensionProjection("countryViewViewIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStatsWithView.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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
