/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.TypeHelper;
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

import java.util.HashMap;
import java.util.stream.Collectors;

import static com.yahoo.elide.core.utils.TypeHelper.getType;

public class MetaDataStoreTest {
    private static MetaDataStore dataStore =
                    new MetaDataStore(
                                    ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example")
                                                    .stream()
                                                    .map(TypeHelper::getType)
                                                    .collect(Collectors.toSet()),
                                    true);

    @BeforeAll
    public static void setup() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(getType(PlayerStatsWithView.class));
        dictionary.bindEntity(getType(PlayerStatsView.class));
        dictionary.bindEntity(getType(PlayerStats.class));
        dictionary.bindEntity(getType(Country.class));
        dictionary.bindEntity(getType(SubCountry.class));
        dictionary.bindEntity(getType(Player.class));
        dictionary.bindEntity(getType(CountryView.class));
        dictionary.bindEntity(getType(CountryViewNested.class));

        dataStore.populateEntityDictionary(dictionary);
    }

    @Test
    public void testSetup() {
        assertNotNull(dataStore);
    }
}
