/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.CountryView;
import com.yahoo.elide.datastores.aggregation.example.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.core.ViewDictionary;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.schema.Schema;

import java.util.HashMap;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public abstract class UnitTest {
    protected static EntityManagerFactory emf;
    protected static Schema playerStatsSchema;
    protected static ViewDictionary dictionary;
    protected static RSQLFilterDialect filterParser;

    protected static final Country HONG_KONG = new Country();
    protected static final Country USA = new Country();

    public static void init() {
        emf = Persistence.createEntityManagerFactory("aggregationStore");
        dictionary = new ViewDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStatsWithView.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(SubCountry.class);
        dictionary.bindEntity(Player.class);
        dictionary.bindView(CountryView.class);
        dictionary.bindView(CountryViewNested.class);
        filterParser = new RSQLFilterDialect(dictionary);

        playerStatsSchema = new SQLSchema(PlayerStats.class, dictionary);

        HONG_KONG.setIsoCode("HKG");
        HONG_KONG.setName("Hong Kong");
        HONG_KONG.setId("344");

        USA.setIsoCode("USA");
        USA.setName("United States");
        USA.setId("840");
    }
}
