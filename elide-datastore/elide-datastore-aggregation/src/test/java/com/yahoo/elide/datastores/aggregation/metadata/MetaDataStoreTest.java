/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.example.metadata.Country;
import com.yahoo.elide.datastores.aggregation.example.metadata.Player;
import com.yahoo.elide.datastores.aggregation.example.metadata.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.metadata.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.metadata.SubCountry;
import com.yahoo.elide.datastores.aggregation.example.metadata.VideoGame;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class MetaDataStoreTest {
    private static MetaDataStore dataStore = new MetaDataStore();

    @BeforeAll
    public static void setup() {
        AggregationDictionary dictionary = new AggregationDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(SubCountry.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(Player.class);
        dictionary.bindEntity(VideoGame.class);

        dataStore.populateEntityDictionary(dictionary);
    }

    @Test
    public void testSetup() {
        assertNotNull(dataStore);
    }
}
