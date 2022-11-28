/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import example.Player;
import example.PlayerRanking;
import example.PlayerStats;
import example.PlayerStatsView;
import example.PlayerStatsWithView;
import example.dimensions.Country;
import example.dimensions.CountryView;
import example.dimensions.CountryViewNested;
import example.dimensions.SubCountry;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class MetaDataStoreTest {
    private ClassScanner scanner = DefaultClassScanner.getInstance();
    private MetaDataStore dataStore;
    public MetaDataStoreTest() {
        Set<Type<?>> types = getClassType(Set.of(
            PlayerStatsWithView.class,
            PlayerStatsView.class,
            PlayerStats.class,
            PlayerRanking.class,
            Country.class,
            SubCountry.class,
            Player.class,
            CountryView.class,
            CountryViewNested.class
        ));

        dataStore = new MetaDataStore(scanner, types, true);

        EntityDictionary dictionary = EntityDictionary.builder().build();
        for (Type<?> type: types) {
            dictionary.bindEntity(type);
        }

        dataStore.populateEntityDictionary(dictionary);

        ConnectionDetails connectionDetails = mock(ConnectionDetails.class);
        QueryEngine engine = new SQLQueryEngine(dataStore, (unused) -> connectionDetails);
    }

    @Test
    public void testSetup() {
        assertNotNull(dataStore);
    }

    @Test
    public void testHiddenFields() {
        Table playerStats = dataStore.getTable(ClassType.of(PlayerStats.class));
        Dimension country = playerStats.getDimension("country");
        Dimension playerRank = playerStats.getDimension("playerRank");
        Metric highScore = playerStats.getMetric("highScore");
        Metric hiddenHighScore = playerStats.getMetric("hiddenHighScore");
        TimeDimension recordedDate = playerStats.getTimeDimension("recordedDate");
        TimeDimension hiddenRecordedDate = playerStats.getTimeDimension("hiddenRecordedDate");

        assertTrue(country.isHidden());
        assertFalse(playerRank.isHidden());
        assertTrue(hiddenHighScore.isHidden());
        assertFalse(highScore.isHidden());
        assertTrue(hiddenRecordedDate.isHidden());
        assertFalse(recordedDate.isHidden());

        assertTrue(playerStats.getColumns().contains(highScore));
        assertTrue(playerStats.getColumns().contains(recordedDate));
        assertTrue(playerStats.getColumns().contains(playerRank));
        assertFalse(playerStats.getColumns().contains(country));
        assertFalse(playerStats.getColumns().contains(hiddenHighScore));
        assertFalse(playerStats.getColumns().contains(hiddenRecordedDate));

        assertTrue(playerStats.getAllColumns().contains(highScore));
        assertTrue(playerStats.getAllColumns().contains(recordedDate));
        assertTrue(playerStats.getAllColumns().contains(playerRank));
        assertTrue(playerStats.getAllColumns().contains(country));
        assertTrue(playerStats.getAllColumns().contains(hiddenHighScore));
        assertTrue(playerStats.getAllColumns().contains(hiddenRecordedDate));

        assertFalse(playerStats.getDimensions().contains(country));
        assertFalse(playerStats.getMetrics().contains(hiddenHighScore));
        assertFalse(playerStats.getTimeDimensions().contains(hiddenRecordedDate));
        assertTrue(playerStats.getMetrics().contains(highScore));
        assertTrue(playerStats.getDimensions().contains(playerRank));
        assertTrue(playerStats.getTimeDimensions().contains(recordedDate));

        assertTrue(playerStats.getAllDimensions().contains(country));
        assertTrue(playerStats.getAllMetrics().contains(hiddenHighScore));
        assertTrue(playerStats.getAllTimeDimensions().contains(hiddenRecordedDate));
        assertTrue(playerStats.getAllMetrics().contains(highScore));
        assertTrue(playerStats.getAllDimensions().contains(playerRank));
        assertTrue(playerStats.getAllTimeDimensions().contains(recordedDate));
    }

    @Test
    public void testHiddenTable() {
        Table player = dataStore.getTable(ClassType.of(Player.class));
        Table playerStats = dataStore.getTable(ClassType.of(PlayerStats.class));
        assertTrue(player.isHidden());
        assertFalse(playerStats.isHidden());

        Namespace namespace = dataStore.getNamespace(ClassType.of(Player.class));
        assertTrue(namespace.getTables().contains(playerStats));
        assertFalse(namespace.getTables().contains(player));
    }
}
