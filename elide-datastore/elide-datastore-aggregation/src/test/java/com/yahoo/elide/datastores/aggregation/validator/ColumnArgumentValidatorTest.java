/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.QueryValidator;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.modelconfig.model.Argument;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.Join;
import com.yahoo.elide.modelconfig.model.NamespaceConfig;
import com.yahoo.elide.modelconfig.model.Table;
import com.yahoo.elide.modelconfig.model.Type;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class ColumnArgumentValidatorTest {

    private final Set<Optimizer> optimizers = new HashSet<>();
    private final QueryValidator queryValidator = new DefaultQueryValidator(EntityDictionary.builder().build());

    private Table.TableBuilder mainTableBuilder, joinTableBuilder;
    private final Argument mainArg1;
    private final Collection<NamespaceConfig> namespaceConfigs;
    private final Function<String, ConnectionDetails> connectionLookup;

    public ColumnArgumentValidatorTest() {
        ConnectionDetails connection = new ConnectionDetails(new HikariDataSource(), SQLDialectFactory.getDefaultDialect());
        Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();
        connectionDetailsMap.put("mycon", connection);
        connectionDetailsMap.put("SalesDBConnection", connection);

        this.namespaceConfigs = Collections.singleton(NamespaceConfig.builder().name("namespace").build());

        this.mainTableBuilder = Table.builder()
                        .name("MainTable")
                        .namespace("namespace");
        this.joinTableBuilder = Table.builder()
                        .name("JoinTable")
                        .namespace("namespace");

        this.mainArg1 = Argument.builder()
                        .name("mainArg1")
                        .type(Type.INTEGER)
                        .values(Collections.emptySet())
                        .defaultValue("")
                        .build();

        connectionLookup = (name) -> connectionDetailsMap.getOrDefault(name, connection);
    }

    @Test
    public void testUndefinedColumnArgsInColumnDefinition() {
        Table mainTable = mainTableBuilder
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{$$column.args.mainArg2}} end")
                                        .argument(mainArg1)
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify column arguments for column: dim1 in table: namespace_MainTable. Argument 'mainArg2' is not defined but found '{{$$column.args.mainArg2}}'.",
                        e.getMessage());
    }

    @Test
    public void testUndefinedColumnArgsInJoinExpression() {
        Table mainTable = mainTableBuilder
                        .join(Join.builder()
                                        .name("join")
                                        .namespace("namespace")
                                        .to("JoinTable")
                                        .definition("start {{$$column.args.mainArg2}} blah")
                                        .build())
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{join.$joinCol}} end")
                                        .argument(mainArg1)
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        tables.add(joinTableBuilder.build());
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify column arguments for column: dim1 in table: namespace_MainTable. Argument 'mainArg2' is not defined but found '{{$$column.args.mainArg2}}'.",
                        e.getMessage());
    }

    @Test
    public void testColumnArgsTypeMismatchForDepenedentColumn() {
        Table mainTable = mainTableBuilder
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{dim2}} end")
                                        .argument(mainArg1)
                                        .build())
                        .dimension(Dimension.builder()
                                        .name("dim2")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("{{$dim2}} blah {{$$column.args.mainArg1}} end")
                                        .argument(Argument.builder()
                                                        .name("mainArg1")
                                                        .type(Type.TEXT)
                                                        .values(Collections.emptySet())
                                                        .defaultValue("")
                                                        .build())
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);

        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify column arguments for column: dim1 in table: namespace_MainTable. Argument type mismatch. Dependent Column: 'dim2' in table: 'namespace_MainTable'"
                        + " has same Argument: 'mainArg1' with type 'TEXT'.",
                        e.getMessage());
    }

    @Test
    public void testMissingRequiredColumnArgs() {
        Table mainTable = mainTableBuilder
               .dimension(Dimension.builder()
                        .name("dim1")
                        .type(Type.BOOLEAN)
                        .values(Collections.emptySet())
                        .tags(Collections.emptySet())
                        .definition("start {{$$column.args.mainArg1}} end")
                        .build())
                .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify column arguments for column: dim1 in table: namespace_MainTable. Argument 'mainArg1' is not defined but found '{{$$column.args.mainArg1}}'.",
                e.getMessage());
    }

    @Test
    public void testRequiredColumnArgsInFilterTemplate() {
        Table mainTable = mainTableBuilder
                .dimension(Dimension.builder()
                        .name("dim1")
                        .type(Type.BOOLEAN)
                        .values(Collections.emptySet())
                        .tags(Collections.emptySet())
                        .definition("start {{$$column.args.mainArg1}} end")
                        .filterTemplate("dim1=={{mainArg1}}")
                        .build())
                .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        assertDoesNotThrow(() -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));
    }

    @Test
    public void testMissingRequiredColumnArgsForDepenedentColumnCase1() {
        Table mainTable = mainTableBuilder
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{dim2}} end")
                                        .argument(mainArg1)
                                        .build())
                        .dimension(Dimension.builder()
                                        .name("dim2")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("{{$dim2}} blah {{$$column.args.dependentArg1}} end")
                                        .argument(Argument.builder()
                                                        .name("dependentArg1")
                                                        .type(Type.INTEGER)
                                                        .values(Collections.emptySet())
                                                        .defaultValue("")
                                                        .build())
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);

        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify column arguments for column: dim1 in table: namespace_MainTable. Argument 'dependentArg1' with type 'INTEGER' is not defined but is"
                        + " required for Dependent Column: 'dim2' in table: 'namespace_MainTable'.",
                        e.getMessage());

        // If 'dim2' is invoked using SQL helper, must not complain.
        mainTable = mainTableBuilder
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{sql column='dim2[dependentArg1:123]'}} end")
                                        .argument(mainArg1)
                                        .build())
                        .dimension(Dimension.builder()
                                        .name("dim2")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("{{$dim2}} blah {{$$column.args.dependentArg1}} end")
                                        .argument(Argument.builder()
                                                        .name("dependentArg1")
                                                        .type(Type.INTEGER)
                                                        .values(Collections.emptySet())
                                                        .defaultValue("")
                                                        .build())
                                        .build())
                        .build();

        tables = new HashSet<>();
        tables.add(mainTable);

        MetaDataStore metaDataStore1 = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        assertDoesNotThrow(() -> new SQLQueryEngine(metaDataStore1, connectionLookup, optimizers, merger, queryValidator));
    }

    @Test
    public void testMissingRequiredColumnArgsForDepenedentColumnCase2() {
        Table mainTable = mainTableBuilder
                        .join(Join.builder()
                                        .name("join")
                                        .namespace("namespace")
                                        .to("JoinTable")
                                        .definition("start {{$$column.args.mainArg1}} blah {{join.$joinCol}} end")
                                        .build())
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{join.joinCol}} end")
                                        .argument(mainArg1)
                                        .build())
                        .build();

        Table joinTable = joinTableBuilder
                        .dimension(Dimension.builder()
                                        .name("joinCol")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("{{$joinCol}} blah {{$$column.args.joinArg1}} end")
                                        .argument(Argument.builder()
                                                        .name("joinArg1")
                                                        .type(Type.INTEGER)
                                                        .values(Collections.emptySet())
                                                        .defaultValue("")
                                                        .build())
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        tables.add(joinTable);

        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify column arguments for column: dim1 in table: namespace_MainTable. Argument 'joinArg1' with type 'INTEGER' is not defined but is"
                        + " required for Dependent Column: 'joinCol' in table: 'namespace_JoinTable'.",
                        e.getMessage());

        // If 'join.joinCol' is invoked using SQL helper, must not complain.
        mainTable = mainTableBuilder
                        .join(Join.builder()
                                        .name("join")
                                        .namespace("namespace")
                                        .to("JoinTable")
                                        .definition("start {{$$column.args.mainArg1}} blah {{join.$joinCol}} end")
                                        .build())
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{sql from='join' column='joinCol[joinArg1:123]'}} end")
                                        .argument(mainArg1)
                                        .build())
                        .build();

        tables = new HashSet<>();
        tables.add(mainTable);
        tables.add(joinTable);

        MetaDataStore metaDataStore1 = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        assertDoesNotThrow(() -> new SQLQueryEngine(metaDataStore1, connectionLookup, optimizers, merger, queryValidator));
    }

    @Test
    public void testInvalidPinnedArgForDepenedentColumn() {
        // 'dim2' is invoked using SQL helper, pinned value is Invalid
        Table mainTable = mainTableBuilder
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$column.args.mainArg1}} blah {{sql column='dim2[dependentArg1:foo]'}} end")
                                        .argument(mainArg1)
                                        .build())
                        .dimension(Dimension.builder()
                                        .name("dim2")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("{{$dim2}} blah {{$$column.args.dependentArg1}} end")
                                        .argument(Argument.builder()
                                                        .name("dependentArg1")
                                                        .type(Type.INTEGER)
                                                        .values(Collections.emptySet())
                                                        .defaultValue("")
                                                        .build())
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);

        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify column arguments for column: dim1 in table: namespace_MainTable. Type mismatch of Fixed value provided for Dependent Column: 'dim2'"
                        + " in table: 'namespace_MainTable'. Pinned Value: 'foo' for Argument 'dependentArg1' with Type 'INTEGER' is invalid.",
                        e.getMessage());
    }
}
