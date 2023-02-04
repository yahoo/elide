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
import org.apache.commons.compress.utils.Sets;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class TableArgumentValidatorTest {

    private final Set<Optimizer> optimizers = new HashSet<>();
    private final QueryValidator queryValidator = new DefaultQueryValidator(EntityDictionary.builder().build());

    private Table.TableBuilder mainTableBuilder;
    private final Collection<NamespaceConfig> namespaceConfigs;
    private final Function<String, ConnectionDetails> connectionLookup;

    public TableArgumentValidatorTest() {
        ConnectionDetails connection = new ConnectionDetails(new HikariDataSource(), SQLDialectFactory.getDefaultDialect());
        Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();
        connectionDetailsMap.put("mycon", connection);
        connectionDetailsMap.put("SalesDBConnection", connection);

        this.namespaceConfigs = Collections.singleton(NamespaceConfig.builder().name("namespace").build());

        this.mainTableBuilder = Table.builder()
                        .name("MainTable")
                        .namespace("namespace")
                        .argument(Argument.builder()
                                        .name("mainArg1")
                                        .type(Type.INTEGER)
                                        .values(Collections.emptySet())
                                        .defaultValue("")
                                        .build())
                        ;

        connectionLookup = (name) -> connectionDetailsMap.getOrDefault(name, connection);
    }

    @Test
    public void testArgumentValues() {

        Table mainTable = mainTableBuilder
                        .argument(Argument.builder()
                                        .name("testArg")
                                        .type(Type.INTEGER)
                                        .values(Sets.newHashSet("1", "2.5"))
                                        .defaultValue("")
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Value: '2.5' for Argument 'testArg' with Type 'INTEGER' is invalid.",
                        e.getMessage());
    }

    @Test
    public void testDefaultValueIsInValues() {

        Table mainTable = mainTableBuilder
                        .argument(Argument.builder()
                                        .name("testArg")
                                        .type(Type.INTEGER)
                                        .values(Sets.newHashSet("1", "2"))
                                        .defaultValue("5")
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Default Value: '5' for Argument 'testArg' with Type 'INTEGER' must match one of these values: [1, 2].",
                        e.getMessage());
    }

    @Test
    public void testDefaultValueMatchesType() {

        Table mainTable = mainTableBuilder
                        .argument(Argument.builder()
                                        .name("testArg")
                                        .type(Type.INTEGER)
                                        .values(Collections.emptySet())
                                        .defaultValue("2.5")
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Default Value: '2.5' for Argument 'testArg' with Type 'INTEGER' is invalid.",
                        e.getMessage());
    }

    @Test
    public void testUndefinedTableArgsInTableSql() {

        Table mainTable = mainTableBuilder
                        .sql("SELECT something {{$$table.args.mainArg1}} blah {{$$table.args.mainArg2}} FROM")
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'mainArg2' is not defined but found '{{$$table.args.mainArg2}}' in table's sql.",
                        e.getMessage());
    }

    @Test
    public void testUndefinedTableArgsInColumnDefinition() {
        Table mainTable = mainTableBuilder
                        .dimension(Dimension.builder()
                                        .name("dim1")
                                        .type(Type.BOOLEAN)
                                        .values(Collections.emptySet())
                                        .tags(Collections.emptySet())
                                        .definition("start {{$$table.args.mainArg1}} blah {{$$table.args.mainArg2}} end")
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'mainArg2' is not defined but found '{{$$table.args.mainArg2}}' in definition of column: 'dim1'.",
                        e.getMessage());
    }

    @Test
    public void testUndefinedTableArgsInJoinExpressions() {
        Table mainTable = mainTableBuilder
                        .join(Join.builder()
                                        .name("join")
                                        .namespace("namespace")
                                        .to("JoinTable")
                                        .definition("start {{$$table.args.mainArg1}} blah {{$$table.args.mainArg2}} end")
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        tables.add(Table.builder()
                        .name("JoinTable")
                        .namespace("namespace")
                        .build());
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'mainArg2' is not defined but found '{{$$table.args.mainArg2}}' in definition of join: 'join'.",
                        e.getMessage());
    }

    @Test
    public void testMissingRequiredTableArgsForJoinTable() {
        Table mainTable = mainTableBuilder
                        .join(Join.builder()
                                        .name("join")
                                        .namespace("namespace")
                                        .to("JoinTable")
                                        .definition("start {{$$table.args.mainArg1}} end")
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        tables.add(Table.builder()
                        .name("JoinTable")
                        .namespace("namespace")
                        .argument(Argument.builder()
                                        .name("joinArg1")
                                        .type(Type.INTEGER)
                                        .values(Collections.emptySet())
                                        .defaultValue("")
                                        .build())
                        .build());

        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'joinArg1' with type 'INTEGER' is not defined but is required by join table: namespace_JoinTable.",
                        e.getMessage());
    }

    @Test
    public void testRequiredTableArgsForJoinTableInFilterTemplate() {
        Table mainTable = Table.builder()
                .name("MainTable")
                .namespace("namespace")
                .filterTemplate("foo>{{filterArg1}}")
                .join(Join.builder()
                        .name("join")
                        .namespace("namespace")
                        .to("JoinTable")
                        .definition("start {{$$table.args.filterArg1}} end")
                        .build())
                .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        tables.add(Table.builder()
                .name("JoinTable")
                .namespace("namespace")
                .build());

        assertDoesNotThrow(() -> {
            new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        });
    }

    @Test
    public void testTableArgsTypeMismatchForJoinTable() {
        Table mainTable = mainTableBuilder
                        .join(Join.builder()
                                        .name("join")
                                        .namespace("namespace")
                                        .to("JoinTable")
                                        .definition("start {{$$table.args.mainArg1}} end")
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.add(mainTable);
        tables.add(Table.builder()
                        .name("JoinTable")
                        .namespace("namespace")
                        .argument(Argument.builder()
                                        .name("mainArg1")
                                        .type(Type.TEXT)
                                        .values(Collections.emptySet())
                                        .defaultValue("")
                                        .build())
                        .build());

        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), tables, this.namespaceConfigs, true);
        QueryPlanMerger merger = new DefaultQueryPlanMerger(metaDataStore);
        Exception e = assertThrows(IllegalStateException.class, () -> new SQLQueryEngine(metaDataStore, connectionLookup, optimizers, merger, queryValidator));

        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument type mismatch. Join table: 'namespace_JoinTable' has same Argument: 'mainArg1' with type 'TEXT'.",
                        e.getMessage());
    }
}
