/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.Table;
import com.yahoo.elide.modelconfig.model.Type;
import org.junit.jupiter.api.Test;

public class TableTypeTest {

    @Test
    void testGetAndSetField() throws Exception {
        Table testTable = Table.builder()
                .dimension(Dimension.builder()
                        .name("dim1")
                        .type(Type.BOOLEAN)
                        .build())
                .build();

        TableType testType = new TableType(testTable);
        DynamicModelInstance testTypeInstance = testType.newInstance();

        Field field = testType.getDeclaredField("dim1");
        field.set(testTypeInstance, true);
        assertTrue((Boolean) field.get(testTypeInstance));
        field.set(testTypeInstance, false);
        assertFalse((Boolean) field.get(testTypeInstance));
    }

    @Test
    void testTableAnnotations() throws Exception {
        Table testTable = Table.builder()
                .cardinality("medium")
                .description("A test table")
                .friendlyName("foo")
                .table("table1")
                .name("Table")
                .schema("db1")
                .category("category1")
                .build();

        TableType testType = new TableType(testTable);

        Include include = (Include) testType.getAnnotation(Include.class);
        assertEquals("Table", include.type());

        FromTable fromTable = (FromTable) testType.getAnnotation(FromTable.class);
        assertEquals("db1.table1", fromTable.name());

        TableMeta tableMeta = (TableMeta) testType.getAnnotation(TableMeta.class);
        assertEquals("foo", tableMeta.friendlyName());
        assertEquals(CardinalitySize.MEDIUM, tableMeta.size());
        assertEquals("A test table", tableMeta.description());
        assertEquals("category1", tableMeta.category());
    }
}
