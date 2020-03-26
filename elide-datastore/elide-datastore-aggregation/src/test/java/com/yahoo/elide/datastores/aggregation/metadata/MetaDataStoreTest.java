/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.CountryView;
import com.yahoo.elide.datastores.aggregation.example.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.utils.ClassScanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class MetaDataStoreTest {
    private static MetaDataStore dataStore =
            new MetaDataStore(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example"));

    @BeforeAll
    public static void setup() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
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
