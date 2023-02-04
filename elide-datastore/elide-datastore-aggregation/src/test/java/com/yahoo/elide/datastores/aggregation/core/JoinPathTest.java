/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import example.Player;
import example.PlayerRanking;
import example.PlayerStats;
import example.PlayerStatsWithView;
import example.dimensions.Country;
import example.dimensions.CountryView;
import example.dimensions.CountryViewNested;
import example.dimensions.SubCountry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

public class JoinPathTest {
    private static MetaDataStore store;

    @BeforeAll
    public static void init() {

        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(PlayerStats.class));
        models.add(ClassType.of(CountryView.class));
        models.add(ClassType.of(Country.class));
        models.add(ClassType.of(SubCountry.class));
        models.add(ClassType.of(Player.class));
        models.add(ClassType.of(PlayerRanking.class));
        models.add(ClassType.of(CountryViewNested.class));
        models.add(ClassType.of(PlayerStatsWithView.class));

        EntityDictionary dictionary = EntityDictionary.builder().build();

        models.stream().forEach(dictionary::bindEntity);

        store = new MetaDataStore(dictionary.getScanner(), models, true);
        store.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        //The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(store, (unused) -> new ConnectionDetails(mockDataSource,
                SQLDialectFactory.getDefaultDialect()));
    }

    @Test
    public void testExtendPath() {
        JoinPath joinPath = new JoinPath(ClassType.of(PlayerStatsWithView.class), store, "countryView");

        JoinPath extended = new JoinPath(ClassType.of(PlayerStatsWithView.class), store, "countryView.nestedView");

        assertEquals(extended, joinPath.extend("countryView.nestedView"));
    }
}
