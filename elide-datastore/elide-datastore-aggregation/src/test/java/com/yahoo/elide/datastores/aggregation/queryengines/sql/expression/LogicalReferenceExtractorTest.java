/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerRanking;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.dimensions.Country;
import com.yahoo.elide.datastores.aggregation.example.dimensions.SubCountry;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

public class LogicalReferenceExtractorTest {

    private ExpressionParser parser;
    private SQLTable playerStats;
    private MetaDataStore metaDataStore;

    public LogicalReferenceExtractorTest() {
        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(PlayerStats.class));
        models.add(ClassType.of(Country.class));
        models.add(ClassType.of(SubCountry.class));
        models.add(ClassType.of(Player.class));
        models.add(ClassType.of(PlayerRanking.class));

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        models.stream().forEach(dictionary::bindEntity);

        metaDataStore = new MetaDataStore(models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        //The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, new ConnectionDetails(mockDataSource,
                SQLDialectFactory.getDefaultDialect()));

        parser = new ExpressionParser(metaDataStore);
        playerStats = (SQLTable) metaDataStore.getTable(ClassType.of(PlayerStats.class));
    }

    @Test
    public void testPhysicalReference() {
        ColumnProjection recordedDate = playerStats.getColumnProjection("recordedDate");

        List<Reference> references = parser.parse(playerStats, recordedDate.getExpression());

        assertTrue(references.size() == 1);

        LogicalReferenceExtractor extractor = new LogicalReferenceExtractor(metaDataStore);
        Set<LogicalReference> logicalReferences = references.get(0).accept(extractor);

        assertTrue(logicalReferences.size() == 0);
    }

    @Test
    public void testJoinReference() {
        ColumnProjection countryIsInUsa = playerStats.getColumnProjection("countryIsInUsa");

        List<Reference> references = parser.parse(playerStats, countryIsInUsa.getExpression());

        assertTrue(references.size() == 1);

        LogicalReferenceExtractor extractor = new LogicalReferenceExtractor(metaDataStore);
        Set<LogicalReference> logicalReferences = references.get(0).accept(extractor);

        assertEquals(0, logicalReferences.size());
    }

    @Test
    public void testLogicalReference() {
        ColumnProjection playerLevel = playerStats.getColumnProjection("playerLevel");

        List<Reference> references = parser.parse(playerStats, playerLevel.getExpression());

        assertTrue(references.size() == 1);

        LogicalReferenceExtractor extractor = new LogicalReferenceExtractor(metaDataStore);
        Set<LogicalReference> logicalReferences = references.get(0).accept(extractor);

        assertEquals(1, logicalReferences.size());
        assertTrue(logicalReferences.contains(LogicalReference
                .builder()
                .source(playerStats)
                .column(playerStats.getColumnProjection("overallRating"))
                .reference(PhysicalReference.builder().source(playerStats).name("overallRating").build())
                .build()));

    }
}
