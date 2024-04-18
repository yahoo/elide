/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.aggregation.annotation.ArgumentDefinition;
import com.paiondata.elide.datastores.aggregation.annotation.DimensionFormula;
import com.paiondata.elide.datastores.aggregation.annotation.Join;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.metadata.enums.ValueType;
import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Id;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

public class SQLColumnProjectionTest {

    private MetaDataStore metaDataStore;

    @Include
    class TableA {
        @Id
        long id;

        @Join(value = "{{$$column.args.foo}} = '123' AND {{$id}} == {{tableB.$id}}")
        TableB tableB;

        @DimensionFormula(value = "{{tableB.dim1}}",
                          arguments = {@ArgumentDefinition(name = "foo", type = ValueType.TEXT)})
        String dim1;

        @DimensionFormula(value = "{{$$column.args.foo}}",
                          arguments = {@ArgumentDefinition(name = "foo", type = ValueType.TEXT)})
        String dim2;
    }

    @Include
    class TableB {
        @Id
        long id;

        @DimensionFormula(value = "{{$foo}}")
        String dim1;
    }

    public SQLColumnProjectionTest() {

        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(TableA.class));
        models.add(ClassType.of(TableB.class));

        EntityDictionary dictionary = EntityDictionary.builder().build();

        models.stream().forEach(dictionary::bindEntity);

        metaDataStore = new MetaDataStore(dictionary.getScanner(), models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        //The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, (unused) -> new ConnectionDetails(mockDataSource,
                SQLDialectFactory.getDefaultDialect()));
    }


    @Test
    public void testColumnThatCannotNest() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableA.class));

        ColumnProjection projection = table.getColumnProjection("dim1");

        assertFalse(projection.canNest(table, metaDataStore));
    }

    @Test
    public void testPhysicalColumnThatCanNest() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableB.class));

        ColumnProjection projection = table.getColumnProjection("dim1");

        assertTrue(projection.canNest(table, metaDataStore));
    }

    @Test
    public void testColumnArgsColumnThatCanNest() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableA.class));

        Map<String, Argument> args = new HashMap<>();
        args.put("foo", Argument.builder().name("foo").value("bar").build());

        ColumnProjection projection = table.getColumnProjection("dim2", args);

        assertTrue(projection.canNest(table, metaDataStore));
    }
}
