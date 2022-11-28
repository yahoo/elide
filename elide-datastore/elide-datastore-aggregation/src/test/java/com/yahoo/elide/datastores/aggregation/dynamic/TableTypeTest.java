/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.dynamic;

import static com.yahoo.elide.datastores.aggregation.annotation.JoinType.INNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.DefaultMetricProjectionMaker;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.Grain;
import com.yahoo.elide.modelconfig.model.Join;
import com.yahoo.elide.modelconfig.model.Measure;
import com.yahoo.elide.modelconfig.model.Table;
import com.yahoo.elide.modelconfig.model.TableSource;
import com.yahoo.elide.modelconfig.model.Type;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        Set<String> tags = new HashSet<>(Arrays.asList("tag1", "tag2"));
        Set<String> hints = new HashSet<>(Arrays.asList("hint1", "hint2"));

        Table testTable = Table.builder()
                .cardinality("medium")
                .description("A test table")
                .friendlyName("foo")
                .table("table1")
                .name("Table")
                .schema("db1")
                .category("category1")
                .readAccess("Admin")
                .dbConnectionName("dbConn")
                .isFact(true)
                .filterTemplate("a==b")
                .tags(tags)
                .hints(hints)
                .build();

        TableType testType = new TableType(testTable);

        Include include = (Include) testType.getAnnotation(Include.class);
        assertEquals("Table", include.name());

        FromTable fromTable = (FromTable) testType.getAnnotation(FromTable.class);
        assertEquals("db1.table1", fromTable.name());
        assertEquals("dbConn", fromTable.dbConnectionName());

        TableMeta tableMeta = (TableMeta) testType.getAnnotation(TableMeta.class);
        assertEquals("foo", tableMeta.friendlyName());
        assertEquals(CardinalitySize.MEDIUM, tableMeta.size());
        assertEquals("A test table", tableMeta.description());
        assertEquals("category1", tableMeta.category());
        assertTrue(tableMeta.isFact());
        assertEquals(tags, new HashSet<>(Arrays.asList(tableMeta.tags())));
        assertEquals(hints, new HashSet<>(Arrays.asList(tableMeta.hints())));
        assertEquals("a==b", tableMeta.filterTemplate());

        ReadPermission readPermission = (ReadPermission) testType.getAnnotation(ReadPermission.class);
        assertEquals("Admin", readPermission.expression());
    }

    @Test
    void testHiddenTableAnnotations() throws Exception {
        Table testTable = Table.builder()
                .cardinality("medium")
                .description("A test table")
                .friendlyName("foo")
                .table("table1")
                .name("Table")
                .hidden(true)
                .schema("db1")
                .category("category1")
                .build();

        TableType testType = new TableType(testTable);

        Include include = (Include) testType.getAnnotation(Include.class);
        assertNotNull(include);

        TableMeta tableMeta = (TableMeta) testType.getAnnotation(TableMeta.class);
        assertNotNull(tableMeta);
        assertTrue(tableMeta.isHidden());
    }

    @Test
    void testTableNameWithoutSchema() throws Exception {
        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .build();

        TableType testType = new TableType(testTable);

        FromTable fromTable = (FromTable) testType.getAnnotation(FromTable.class);
        assertEquals("table1", fromTable.name());
    }

    @Test
    void testTableSQL() throws Exception {
        Table testTable = Table.builder()
                .sql("SELECT * FROM FOO")
                .name("Table")
                .dbConnectionName("dbConn")
                .build();

        TableType testType = new TableType(testTable);

        FromSubquery fromSubquery = (FromSubquery) testType.getAnnotation(FromSubquery.class);
        assertEquals("SELECT * FROM FOO", fromSubquery.sql());
        assertEquals("dbConn", fromSubquery.dbConnectionName());
    }

    @Test
    void testIdField() throws Exception {
        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .build();

        TableType testType = new TableType(testTable);

        assertTrue(testType.getFields().length == 1);
        Field field = testType.getDeclaredField("id");

        assertNotNull(field);
        Id id = field.getAnnotation(Id.class);
        assertNotNull(id);
    }

    @Test
    void testMeasureAnnotations() throws Exception {
        Set<String> tags = new HashSet<>(Arrays.asList("tag1", "tag2"));

        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .measure(Measure.builder()
                        .type(Type.MONEY)
                        .category("category1")
                        .definition("SUM{{  price}}")
                        .hidden(false)
                        .friendlyName("Price")
                        .name("price")
                        .readAccess("Admin")
                        .description("A measure")
                        .tags(tags)
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("price");
        assertNotNull(field);

        ReadPermission readPermission = field.getAnnotation(ReadPermission.class);
        assertEquals("Admin", readPermission.expression());

        ColumnMeta columnMeta = field.getAnnotation(ColumnMeta.class);
        assertEquals("A measure", columnMeta.description());
        assertEquals("category1", columnMeta.category());
        assertEquals("Price", columnMeta.friendlyName());
        assertEquals(CardinalitySize.UNKNOWN, columnMeta.size());
        assertEquals(tags, new HashSet<>(Arrays.asList(columnMeta.tags())));

        MetricFormula metricFormula = field.getAnnotation(MetricFormula.class);
        assertEquals("SUM{{price}}", metricFormula.value());
        assertEquals(DefaultMetricProjectionMaker.class, metricFormula.maker());
    }

    @Test
    void testDimensionAnnotations() throws Exception {
        Set<String> tags = new HashSet<>(Arrays.asList("tag1", "tag2"));

        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .dimension(Dimension.builder()
                        .type(Type.TEXT)
                        .category("category1")
                        .definition("{{region}}")
                        .hidden(false)
                        .friendlyName("Region")
                        .name("region")
                        .readAccess("Admin")
                        .description("A dimension")
                        .tags(tags)
                        .cardinality("small")
                        .tableSource(TableSource.builder()
                                .table("region")
                                .column("id")
                                .build())
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("region");
        assertNotNull(field);

        ReadPermission readPermission = field.getAnnotation(ReadPermission.class);
        assertEquals("Admin", readPermission.expression());

        ColumnMeta columnMeta = field.getAnnotation(ColumnMeta.class);
        assertEquals("A dimension", columnMeta.description());
        assertEquals("category1", columnMeta.category());
        assertEquals("Region", columnMeta.friendlyName());
        assertEquals(CardinalitySize.SMALL, columnMeta.size());
        assertEquals(tags, new HashSet<>(Arrays.asList(columnMeta.tags())));

        DimensionFormula dimensionFormula = field.getAnnotation(DimensionFormula.class);
        assertEquals("{{region}}", dimensionFormula.value());
    }

    @Test
    void testDimensionTableValues() throws Exception {
        Set<String> values = new HashSet<>(Arrays.asList("DIM1", "DIM2"));

        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .dimension(Dimension.builder()
                        .type(Type.COORDINATE)
                        .name("location")
                        .values(values)
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("location");
        assertNotNull(field);

        ColumnMeta columnMeta = field.getAnnotation(ColumnMeta.class);
        assertEquals(values, new HashSet<>(Arrays.asList(columnMeta.values())));
    }

    @Test
    void testTimeDimensionAnnotations() throws Exception {
        Set<String> tags = new HashSet<>(Arrays.asList("tag1", "tag2"));

        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .dimension(Dimension.builder()
                        .type(Type.TIME)
                        .category("category1")
                        .definition("{{createdOn  }}")
                        .hidden(false)
                        .friendlyName("Created On")
                        .name("createdOn")
                        .readAccess("Admin")
                        .description("A time dimension")
                        .tags(tags)
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("createdOn");
        assertNotNull(field);

        ReadPermission readPermission = field.getAnnotation(ReadPermission.class);
        assertEquals("Admin", readPermission.expression());

        ColumnMeta columnMeta = field.getAnnotation(ColumnMeta.class);
        assertEquals("A time dimension", columnMeta.description());
        assertEquals("category1", columnMeta.category());
        assertEquals("Created On", columnMeta.friendlyName());
        assertEquals(CardinalitySize.UNKNOWN, columnMeta.size());
        assertEquals(tags, new HashSet<>(Arrays.asList(columnMeta.tags())));

        DimensionFormula dimensionFormula = field.getAnnotation(DimensionFormula.class);
        assertEquals("{{createdOn}}", dimensionFormula.value());

        Temporal temporal = field.getAnnotation(Temporal.class);
        assertEquals("UTC", temporal.timeZone());
        assertTrue(temporal.grains().length == 0);
    }

    @Test
    void testMultipleTimeDimensionGrains() throws Exception {

        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .dimension(Dimension.builder()
                        .type(Type.TIME)
                        .definition("{{createdOn}}")
                        .name("createdOn")
                        .grain(Grain.builder()
                                .sql("some sql")
                                .type(Grain.GrainType.DAY)
                                .build())
                        .grain(Grain.builder()
                                .sql("some other sql")
                                .type(Grain.GrainType.YEAR)
                                .build())
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("createdOn");
        assertNotNull(field);

        Temporal temporal = field.getAnnotation(Temporal.class);
        assertEquals("UTC", temporal.timeZone());
        assertTrue(temporal.grains().length == 2);
        assertEquals("some sql", temporal.grains()[0].expression());
        assertEquals("some other sql", temporal.grains()[1].expression());
        assertEquals(TimeGrain.DAY, temporal.grains()[0].grain());
        assertEquals(TimeGrain.YEAR, temporal.grains()[1].grain());
    }

    @Test
    void testJoinField() throws Exception {
        Table testTable1 = Table.builder()
                .name("table1")
                .join(Join.builder()
                        .definition("{{id }} = {{ table2.id}}")
                        .kind(Join.Kind.TOONE)
                        .type(Join.Type.INNER)
                        .name("join1")
                        .to("table2.dim2")
                        .build())
                .build();

        Table testTable2 = Table.builder()
                .name("table2")
                .dimension(Dimension.builder()
                        .name("dim2")
                        .type(Type.BOOLEAN)
                        .build())
                .build();

        TableType testType1 = new TableType(testTable1);
        TableType testType2 = new TableType(testTable2);

        Map<String, com.yahoo.elide.core.type.Type<?>> tables = new HashMap<>();
        tables.put("table1", testType1);
        tables.put("table2", testType2);

        testType1.resolveJoins(tables);

        Field field = testType1.getDeclaredField("join1");
        assertNotNull(field);

        com.yahoo.elide.datastores.aggregation.annotation.Join join = field.getAnnotation(
                com.yahoo.elide.datastores.aggregation.annotation.Join.class);

        assertEquals(INNER, join.type());
        assertEquals("{{id}} = {{table2.id}}", join.value());
    }

    @Test
    void testHiddenDimension() throws Exception {
        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .dimension(Dimension.builder()
                        .name("dim1")
                        .type(Type.BOOLEAN)
                        .hidden(true)
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("dim1");
        assertNotNull(field);

        ColumnMeta columnMeta = field.getAnnotation(ColumnMeta.class);
        assertNotNull(columnMeta);
        assertTrue(columnMeta.isHidden());
    }

    @Test
    void testHiddenMeasure() throws Exception {
        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .measure(Measure.builder()
                        .name("measure1")
                        .type(Type.BOOLEAN)
                        .hidden(true)
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("measure1");
        assertNotNull(field);

        ColumnMeta columnMeta = field.getAnnotation(ColumnMeta.class);
        assertNotNull(columnMeta);
        assertTrue(columnMeta.isHidden());
    }

    @Test
    void testInvalidResolver() throws Exception {
        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .measure(Measure.builder()
                        .name("measure1")
                        .type(Type.BOOLEAN)
                        .maker("does.not.exist.class")
                        .build())
                .build();

        TableType testType = new TableType(testTable);
        Field field = testType.getDeclaredField("measure1");

        MetricFormula metricFormula = field.getAnnotation(MetricFormula.class);

        assertThrows(IllegalStateException.class, () -> metricFormula.maker());
    }

    @Test
    void testEnumeratedDimension() throws Exception {
        Table testTable = Table.builder()
                .table("table1")
                .name("Table")
                .dimension(Dimension.builder()
                        .name("dim1")
                        .type("ENUM_ORDINAL")
                        .values(Set.of("A", "B", "C"))
                        .build())
                .build();

        TableType testType = new TableType(testTable);

        Field field = testType.getDeclaredField("dim1");
        assertNotNull(field);

        Enumerated enumerated = field.getAnnotation(Enumerated.class);
        assertNotNull(enumerated);
        assertEquals(EnumType.ORDINAL, enumerated.value());
    }
}
