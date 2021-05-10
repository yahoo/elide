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
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerRanking;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.example.dimensions.Country;
import com.yahoo.elide.datastores.aggregation.example.dimensions.CountryView;
import com.yahoo.elide.datastores.aggregation.example.dimensions.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.dimensions.SubCountry;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        models.stream().forEach(dictionary::bindEntity);

        store = new MetaDataStore(models, true);
        store.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        //The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(store, new ConnectionDetails(mockDataSource,
                SQLDialectFactory.getDefaultDialect()));
    }

    @Test
    public void testExtendPath() {
        JoinPath joinPath = new JoinPath(ClassType.of(PlayerStatsWithView.class), store, "countryView");

        JoinPath extended = new JoinPath(ClassType.of(PlayerStatsWithView.class), store, "countryView.nestedView");

        assertEquals(extended, joinPath.extend("countryView.nestedView"));
    }
}
