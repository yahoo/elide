/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.example.Continent;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerRanking;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ColumnDefinition;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.TableContext;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLPhysicalColumnProjection;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

public class SQLReferenceTableTest {

    private SQLReferenceTable lookupTable;
    private SQLTable playerStats;

    public SQLReferenceTableTest() {
        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(PlayerStats.class));
        models.add(ClassType.of(Country.class));
        models.add(ClassType.of(Continent.class));
        models.add(ClassType.of(SubCountry.class));
        models.add(ClassType.of(Player.class));
        models.add(ClassType.of(PlayerRanking.class));

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        models.stream().forEach(dictionary::bindEntity);

        MetaDataStore metaDataStore = new MetaDataStore(models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        //The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, new ConnectionDetails(mockDataSource,
                SQLDialectFactory.getDefaultDialect()));

        lookupTable = new SQLReferenceTable(metaDataStore);

        playerStats = (SQLTable) metaDataStore.getTable(ClassType.of(PlayerStats.class));
    }

    @Test
    public void testNoJoins() {
        Set<SQLColumnProjection> joinProjections =
                lookupTable.getResolvedJoinProjections(playerStats, "overallRating");

        assertEquals(0, joinProjections.size());
    }

    @Test
    public void testJoin() {
        Set<SQLColumnProjection> joinProjections =
                lookupTable.getResolvedJoinProjections(playerStats, "playerName");

        SQLPhysicalColumnProjection expected = new SQLPhysicalColumnProjection("player_id");

        assertEquals(1, joinProjections.size());
        assertEquals(expected, joinProjections.iterator().next());
    }

    @Test
    public void testTableContext() {
        TableContext tableContext = lookupTable.getGlobalTableContext(playerStats);

        Map<String, Object> tableDefaultArgs = tableContext.getDefaultTableArgs();

        // For primary table, {{$$table.args.beginDate}} should resolve to "2020-01-01"
        assertEquals(1, tableDefaultArgs.size());
        assertEquals("2020-01-01", tableDefaultArgs.get("beginDate"));

        // For 'playerRank' column, {{$$column.args.minRanking}} should resolve to "1"
        ColumnDefinition columnDef = (ColumnDefinition) tableContext.getColumnDefinition("playerRank");
        Map<String, Object> columnDefaultArgs = columnDef.getDefaultColumnArgs();
        assertEquals(1, columnDefaultArgs.size());
        assertEquals("1", columnDefaultArgs.get("minRanking"));
    }
}
