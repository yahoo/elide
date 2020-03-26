/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.example.Continent;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.CountryView;
import com.yahoo.elide.datastores.aggregation.example.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.utils.ClassScanner;

import java.util.Collections;
import java.util.HashMap;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public abstract class SQLUnitTest {
    protected static EntityManagerFactory emf;
    protected static Table playerStatsTable;
    protected static EntityDictionary dictionary;
    protected static RSQLFilterDialect filterParser;
    protected static MetaDataStore metaDataStore;

    protected static final Country HONG_KONG = new Country();
    protected static final Country USA = new Country();
    protected static final Continent ASIA = new Continent();
    protected static final Continent NA = new Continent();

    protected static QueryEngine engine;

    public static void init() {
        metaDataStore = new MetaDataStore(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example"));

        emf = Persistence.createEntityManagerFactory("aggregationStore");
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStatsWithView.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(SubCountry.class);
        dictionary.bindEntity(Player.class);
        dictionary.bindEntity(CountryView.class);
        dictionary.bindEntity(CountryViewNested.class);
        dictionary.bindEntity(Continent.class);
        filterParser = new RSQLFilterDialect(dictionary);

        metaDataStore.populateEntityDictionary(dictionary);

        engine = new SQLQueryEngine(metaDataStore, emf);
        playerStatsTable = engine.getTable("playerStats");

        ASIA.setName("Asia");
        ASIA.setId("1");

        NA.setName("North America");
        NA.setId("2");

        HONG_KONG.setIsoCode("HKG");
        HONG_KONG.setName("Hong Kong");
        HONG_KONG.setId("344");
        HONG_KONG.setContinent(ASIA);

        USA.setIsoCode("USA");
        USA.setName("United States");
        USA.setId("840");
        USA.setContinent(NA);
    }

    public static ColumnProjection toProjection(Dimension dimension) {
        return ColumnProjection.toProjection(dimension, dimension.getName());
    }

    public static TimeDimensionProjection toProjection(TimeDimension dimension, TimeGrain grain) {
        return ColumnProjection.toProjection(dimension, grain, dimension.getName());
    }

    public static MetricFunctionInvocation invoke(Metric metric) {
        return metric.getMetricFunction().invoke(Collections.emptySet(), metric.getName());
    }
}
