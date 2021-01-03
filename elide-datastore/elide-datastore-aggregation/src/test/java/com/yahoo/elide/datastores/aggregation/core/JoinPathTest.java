/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import static com.yahoo.elide.core.utils.TypeHelper.getType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.example.CountryView;
import com.yahoo.elide.datastores.aggregation.example.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class JoinPathTest {
    private static EntityDictionary dictionary;
    private static Type<?> playerStatsWithViewType = getType(PlayerStatsWithView.class);
    private static Type<?> countryViewType = getType(CountryView.class);
    private static Type<?> countryViewNestedType = getType(CountryViewNested.class);

    @BeforeAll
    public static void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(playerStatsWithViewType);
        dictionary.bindEntity(countryViewType);
        dictionary.bindEntity(countryViewNestedType);
    }

    @Test
    public void testExtendPath() {
        JoinPath joinPath = new JoinPath(playerStatsWithViewType, dictionary, "countryView");

        JoinPath extended = new JoinPath(playerStatsWithViewType, dictionary, "countryView.nestedView");

        assertEquals(extended, joinPath.extend("countryView.nestedView", dictionary));
    }
}
