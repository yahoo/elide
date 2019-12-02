/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.NonEntityDictionary;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.CountryView;
import com.yahoo.elide.datastores.aggregation.example.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MetaDataStoreTest {
    private static MetaDataStore dataStore = new MetaDataStore(PlayerStats.class.getPackage());

    @BeforeAll
    public static void setup() {
        NonEntityDictionary dictionary = new NonEntityDictionary();
        dictionary.bindEntity(PlayerStatsWithView.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(SubCountry.class);
        dictionary.bindEntity(Player.class);
        dictionary.bindEntity(CountryView.class);
        dictionary.bindEntity(CountryViewNested.class);

        dataStore.populateEntityDictionary(dictionary);
    }

    @Test
    public void testSetup() {
        assertNotNull(dataStore);
    }
}
