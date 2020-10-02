/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceVisitor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.Id;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SqlReferenceVisitorTest {

    private MetaDataStore store;

    @FromTable(name = "test_table")
    @Include
    public class TestModel {
        @Id
        private long id;

        @Join("%from.dimension1 = %join.dimension1")
        private JoinModel joinModel;

        //Logical name matches physical name
        @DimensionFormula("{{dimension1}}")
        private String dimension1;

        //Logical name does not match physical name
        @DimensionFormula("{{someColumn}}")
        private String dimension2;

        //Test a join to another table with a matching column name.
        @DimensionFormula("{{joinModel.dimension3}}")
        private String dimension3;
    }

    @FromTable(name = "join_table")
    @Include
    public class JoinModel {
        @Id
        private long id;

        //Logical name matches physical name
        @DimensionFormula("{{dimension3}}")
        private String dimension3;
    }

    @BeforeAll
    public void init() {
        store = new MetaDataStore(Sets.newHashSet(TestModel.class, JoinModel.class));
        Table table1 = new SQLTable(TestModel.class, store.getDictionary());
        Table table2 = new SQLTable(JoinModel.class, store.getDictionary());
        store.addTable(table1);
        store.addTable(table2);
    }

    @Test
    public void testMatchingPhysicalReference() {
        SQLReferenceVisitor visitor = new SQLReferenceVisitor(store, "test_table");

        Column column = store.getColumn(TestModel.class, "dimension1");

        String actual = visitor.visitColumn(column);

        assertEquals("test_table.dimension1", actual);
    }

    @Test
    public void testMismatchingPhysicalReference() {
        SQLReferenceVisitor visitor = new SQLReferenceVisitor(store, "test_table");

        Column column = store.getColumn(TestModel.class, "dimension2");

        String actual = visitor.visitColumn(column);

        assertEquals("test_table.someColumn", actual);
    }

    @Test
    public void testJoinReference() {
        SQLReferenceVisitor visitor = new SQLReferenceVisitor(store, "join_table");

        Column column = store.getColumn(TestModel.class, "dimension3");

        String actual = visitor.visitColumn(column);

        assertEquals("join_table_joinModel.dimension3", actual);
    }
}
