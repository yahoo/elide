/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import example.Player;
import example.PlayerStats;
import example.PlayerStatsView;
import example.PlayerStatsWithView;
import example.dimensions.Country;
import example.dimensions.CountryView;
import example.dimensions.CountryViewNested;
import example.dimensions.SubCountry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;

public class MetaDataStoreTest {
    private static ClassScanner scanner = DefaultClassScanner.getInstance();
    private static MetaDataStore dataStore = new MetaDataStore(scanner,
                    getClassType(scanner.getAllClasses("example")), true);

    @BeforeAll
    public static void setup() {
        EntityDictionary dictionary = EntityDictionary.builder().build();
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
