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
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.Cache;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.utils.ClassScanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;
import java.util.HashMap;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
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

    protected QueryEngine.Transaction transaction;

    public static void init(Cache cache) {
        emf = Persistence.createEntityManagerFactory("aggregationStore");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery("DROP ALL OBJECTS;").executeUpdate();
        em.createNativeQuery("RUNSCRIPT FROM 'classpath:create_tables.sql'").executeUpdate();
        em.getTransaction().commit();

        metaDataStore = new MetaDataStore(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example"));

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

        engine = new SQLQueryEngine(metaDataStore, emf, cache);
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

    @BeforeEach
    public void begin() {
        transaction = engine.beginTransaction();
    }

    @AfterEach
    public void end() {
        transaction.close();
    }

    public static ColumnProjection toProjection(Dimension dimension) {
        return engine.constructDimensionProjection(dimension, dimension.getName(), Collections.emptyMap());
    }

    public static TimeDimensionProjection toProjection(TimeDimension dimension, TimeGrain grain) {
        return engine.constructTimeDimensionProjection(
                dimension,
                dimension.getName(),
                Collections.singletonMap("grain", Argument.builder().name("grain").value(grain).build()));
    }

    public static MetricProjection invoke(Metric metric) {
        return engine.constructMetricProjection(metric, metric.getName(), Collections.emptyMap());
    }

    protected static List<Object> toList(Iterable<Object> data) {
        return StreamSupport.stream(data.spliterator(), false)
                .collect(Collectors.toList());
    }
}
