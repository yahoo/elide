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
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import example.Player;
import example.PlayerRanking;
import example.PlayerStats;
import example.dimensions.Country;
import example.dimensions.SubCountry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

public class ExpressionParserTest {

    private ExpressionParser parser;
    private SQLTable playerStats;
    private SQLTable country;
    private MetaDataStore metaDataStore;
    private JoinReference countryInUSAJoinReference;

    public ExpressionParserTest() {
        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(PlayerStats.class));
        models.add(ClassType.of(Country.class));
        models.add(ClassType.of(SubCountry.class));
        models.add(ClassType.of(Player.class));
        models.add(ClassType.of(PlayerRanking.class));

        EntityDictionary dictionary = EntityDictionary.builder().build();

        models.stream().forEach(dictionary::bindEntity);

        metaDataStore = new MetaDataStore(dictionary.getScanner(), models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        //The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, (unused) -> new ConnectionDetails(mockDataSource,
                SQLDialectFactory.getDefaultDialect()));

        parser = new ExpressionParser(metaDataStore);
        playerStats = (SQLTable) metaDataStore.getTable(ClassType.of(PlayerStats.class));
        country = (SQLTable) metaDataStore.getTable(ClassType.of(Country.class));

        countryInUSAJoinReference = JoinReference
                .builder()
                .source(playerStats)
                .path(new JoinPath(ClassType.of(PlayerStats.class), metaDataStore, "country.inUsa"))
                .reference(LogicalReference
                        .builder()
                        .source(country)
                        .column(country.getColumnProjection("inUsa"))
                        .reference(LogicalReference
                                .builder()
                                .source(country)
                                .column(country.getColumnProjection("name"))
                                .reference(PhysicalReference
                                        .builder()
                                        .source(country)
                                        .name("name")
                                        .build()
                                )
                                .build()
                        )
                        .build())
                .build();
    }

    @Test
    public void testPhysicalExpression() {
        ColumnProjection recordedDate = playerStats.getColumnProjection("recordedDate");

        List<Reference> references = parser.parse(playerStats, recordedDate.getExpression());

        assertTrue(references.size() == 1);
        assertEquals(PhysicalReference.builder()
                .name("recordedDate")
                .source(playerStats)
                .build(), references.get(0));
    }

    @Test
    public void testLogicalExpression() {
        ColumnProjection playerLevel = playerStats.getColumnProjection("playerLevel");

        List<Reference> references = parser.parse(playerStats, playerLevel.getExpression());

        assertTrue(references.size() == 1);
        assertEquals(LogicalReference
                .builder()
                .source(playerStats)
                .column(playerStats.getColumnProjection("overallRating"))
                .reference(PhysicalReference
                        .builder()
                        .source(playerStats)
                        .name("overallRating")
                        .build())
                .build(), references.get(0));
    }

    @Test
    public void testJoinExpression() {
        ColumnProjection countryInUsa = playerStats.getColumnProjection("countryIsInUsa");

        List<Reference> references = parser.parse(playerStats, countryInUsa.getExpression());

        assertTrue(references.size() == 1);
        assertEquals(countryInUSAJoinReference, references.get(0));
    }

    @Test
    public void testMultipleReferences() {
        List<Reference> references = parser.parse(playerStats, "{{$country_id}} = {{country.$id}}");

        assertTrue(references.size() == 2);

        assertEquals(PhysicalReference.builder()
                .name("country_id")
                .source(playerStats)
                .build(), references.get(0));

        assertEquals(JoinReference
                .builder()
                .source(playerStats)
                .path(new JoinPath(ClassType.of(PlayerStats.class), metaDataStore, "country.$id"))
                .reference(PhysicalReference
                        .builder()
                        .source(country)
                        .name("id")
                        .build())
                .build(), references.get(1));
    }

    @Test
    public void testMultipleReferencesWithHelper() {
        List<Reference> references = parser.parse(playerStats, "{{$country_id}} = {{country.$id}} AND {{sql from='country' column='inUsa'}} = true");

        assertTrue(references.size() == 3);

        assertEquals(PhysicalReference.builder()
                .name("country_id")
                .source(playerStats)
                .build(), references.get(0));

        assertEquals(JoinReference
                .builder()
                .source(playerStats)
                .path(new JoinPath(ClassType.of(PlayerStats.class), metaDataStore, "country.$id"))
                .reference(PhysicalReference
                        .builder()
                        .source(country)
                        .name("id")
                        .build())
                .build(), references.get(1));

        assertEquals(countryInUSAJoinReference, references.get(2));

    }

    @Test
    public void testArgReferences() {
        List<Reference> references = parser.parse(playerStats, "{{$country_id}} with {{$$column.expr}} = {{$$column.args.foo}} OR {{$$table.args.bar}}");

        assertTrue(references.size() == 3);

        assertEquals(PhysicalReference.builder()
                .name("country_id")
                .source(playerStats)
                .build(), references.get(0));

        assertEquals(ColumnArgReference.builder()
                .argName("foo")
                .build(), references.get(1));

        assertEquals(TableArgReference.builder()
                .argName("bar")
                .build(), references.get(2));
    }
}
