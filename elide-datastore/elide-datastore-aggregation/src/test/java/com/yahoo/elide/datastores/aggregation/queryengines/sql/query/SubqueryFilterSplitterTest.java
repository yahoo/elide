/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerRanking;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

public class SubqueryFilterSplitterTest {

    private MetaDataStore metaDataStore;
    private SQLReferenceTable lookupTable;
    private RSQLFilterDialect dialect;
    private static final Type<PlayerStats> PLAYER_STATS_TYPE = ClassType.of(PlayerStats.class);

    public SubqueryFilterSplitterTest() {

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

        lookupTable = new SQLReferenceTable(metaDataStore);
        dialect = new RSQLFilterDialect(dictionary);
    }


    @Test
    public void testSinglePredicateNoJoin() throws Exception {
        FilterExpression expression = parse("overallRating=='Foo'");

        Pair<FilterExpression, FilterExpression> splitExpressions =
                SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, expression);

        assertNull(splitExpressions.getLeft());
        assertEquals(expression, splitExpressions.getRight());
    }

    @Test
    public void testSinglePredicateWithJoin() throws Exception {
        FilterExpression expression = parse("countryUnSeats>3");

        Pair<FilterExpression, FilterExpression> splitExpressions =
                SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, expression);

        assertNull(splitExpressions.getRight());
        assertEquals(expression, splitExpressions.getLeft());
    }

    @Test
    public void testSplitByAnd() throws Exception {
        FilterExpression expression = parse("countryUnSeats>3;overallRating=='Foo'");
        FilterExpression expectedOuter = parse("countryUnSeats>3");
        FilterExpression expectedInner = parse("overallRating=='Foo'");

        Pair<FilterExpression, FilterExpression> splitExpressions =
                SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, expression);

        assertEquals(expectedOuter, splitExpressions.getLeft());
        assertEquals(expectedInner, splitExpressions.getRight());
    }

    @Test
    public void testSplitByOr() throws Exception {
        FilterExpression expression = parse("countryUnSeats>3,overallRating=='Foo'");

        Pair<FilterExpression, FilterExpression> splitExpressions =
                SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, expression);

        assertEquals(expression, splitExpressions.getLeft());
        assertNull(splitExpressions.getRight());
    }

    @Test
    public void testCompoundSplitByAnd() throws Exception {
        FilterExpression expression = parse(
                "(countryUnSeats>3,overallRating=='Foo');(overallRating=='Bar',overallRating=='Blah')");

        FilterExpression expectedOuter = parse("countryUnSeats>3,overallRating=='Foo'");
        FilterExpression expectedInner = parse("overallRating=='Bar',overallRating=='Blah'");

        Pair<FilterExpression, FilterExpression> splitExpressions =
                SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, expression);

        assertEquals(expectedOuter, splitExpressions.getLeft());
        assertEquals(expectedInner, splitExpressions.getRight());
    }

    private FilterExpression parse(String filter) throws ParseException {
        return dialect.parse(PLAYER_STATS_TYPE, new HashSet<>(), filter, NO_VERSION);
    }
}
