/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.example.CountryView;
import com.yahoo.elide.datastores.aggregation.example.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class JoinPathTest {
    private static EntityDictionary dictionary;

    @BeforeAll
    public static void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStatsWithView.class);
        dictionary.bindEntity(CountryView.class);
        dictionary.bindEntity(CountryViewNested.class);
    }

    @Test
    public void testExtendPath() {
        JoinPath joinPath = new JoinPath(PlayerStatsWithView.class, dictionary, "countryView");

        JoinPath extended = new JoinPath(PlayerStatsWithView.class, dictionary, "countryView.nestedView");

        assertEquals(extended, joinPath.extend("countryView.nestedView", dictionary));
    }
}
