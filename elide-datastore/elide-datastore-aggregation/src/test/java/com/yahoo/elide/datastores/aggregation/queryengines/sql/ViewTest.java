/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.metric.Sum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ViewTest extends UnitTest {
    protected static Schema playerStatsWithViewSchema;

    @BeforeAll
    public static void init() {
        UnitTest.init();
        playerStatsWithViewSchema = new SQLSchema(PlayerStatsWithView.class, dictionary);
    }

    @Test
    public void testViewRelationFailure() throws Exception {
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsWithViewSchema)
                .metric(playerStatsWithViewSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsWithViewSchema.getDimension("countryView"))
                .sorting(new Sorting(sortMap))
                .build();

        assertThrows(InvalidPredicateException.class, () -> engine.executeQuery(query));
    }

    @Test
    public void testViewAttribute() throws Exception {
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsWithViewSchema)
                .metric(playerStatsWithViewSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsWithViewSchema.getDimension("countryViewIsoCode"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(276);
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
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsWithViewSchema)
                .metric(playerStatsWithViewSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsWithViewSchema.getDimension("countryViewViewIsoCode"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(276);
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
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsWithViewSchema)
                .metric(playerStatsWithViewSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsWithViewSchema.getDimension("countryViewRelationshipIsoCode"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(276);
        usa0.setCountryViewRelationshipIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewRelationshipIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testSortingViewAttribute() throws Exception {
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryView.isoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsWithViewSchema)
                .metric(playerStatsWithViewSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsWithViewSchema.getDimension("countryViewRelationshipIsoCode"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(276);
        usa0.setCountryViewRelationshipIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewRelationshipIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testSortingNestedViewAttribute() throws Exception {
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryView.nestedView.isoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsWithViewSchema)
                .metric(playerStatsWithViewSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsWithViewSchema.getDimension("countryViewRelationshipIsoCode"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(276);
        usa0.setCountryViewRelationshipIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewRelationshipIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }

    @Test
    public void testSortingNestedRelationshipAttribute() throws Exception {
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryView.nestedRelationship.isoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsWithViewSchema)
                .metric(playerStatsWithViewSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsWithViewSchema.getDimension("countryViewRelationshipIsoCode"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsWithView usa0 = new PlayerStatsWithView();
        usa0.setId("0");
        usa0.setLowScore(276);
        usa0.setCountryViewRelationshipIsoCode("USA");

        PlayerStatsWithView hk1 = new PlayerStatsWithView();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setCountryViewRelationshipIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStatsWithView actualStats1 = (PlayerStatsWithView) results.get(0);
        assertNull(actualStats1.getCountry());
    }
}
