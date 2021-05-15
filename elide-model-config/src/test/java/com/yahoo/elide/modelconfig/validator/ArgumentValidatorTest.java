/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.modelconfig.model.Argument;
import com.yahoo.elide.modelconfig.model.ElideTableConfig;
import com.yahoo.elide.modelconfig.model.Join;
import com.yahoo.elide.modelconfig.model.Table;
import com.yahoo.elide.modelconfig.model.Table.TableBuilder;
import com.yahoo.elide.modelconfig.model.TableSource;
import com.yahoo.elide.modelconfig.model.Type;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ArgumentValidatorTest {

    private TableBuilder mainTableBuilder;
    private Set<Table> tables;
    private ElideTableConfig elideTableConfig;
    private EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

    public ArgumentValidatorTest() {

        mainTableBuilder = Table.builder()
                        .name("MainTable")
                        .namespace("namespace")
                        .argument(Argument.builder()
                                        .name("mainArg1")
                                        .type(Type.INTEGER)
                                        .build())
                        .join(Join.builder()
                                        .name("join")
                                        .namespace("namespace")
                                        .to("JoinTable")
                                        .definition("start {{$$table.args.mainArg1}} blah {{$$table.args.mainArg2}} end")
                                        .build());

        tables = new HashSet<>();
        tables.add(Table.builder()
                        .name("JoinTable")
                        .namespace("namespace")
                        .argument(Argument.builder()
                                        .name("joinArg1")
                                        .type(Type.INTEGER)
                                        .build())
                        .build());

        elideTableConfig = new ElideTableConfig(tables);
    }

    @Test
    public void testDuplicateArgumentName() {
        // mainTable already has an argument 'mainArg1' with type Integer
        // Adding same argument 'mainArg1' with type 'INTEGER'.
        Table mainTable = mainTableBuilder
                        .argument(Argument.builder()
                                        .name("mainArg1")
                                        .type(Type.TEXT)
                                        .build())
                        .build();

        TableArgumentValidator argValidator = new TableArgumentValidator(elideTableConfig, dictionary, mainTable);
        Exception e = assertThrows(IllegalStateException.class, () -> argValidator.validate());
        assertEquals("For table: namespace_MainTable, Multiple Table Arguments found with the same name: mainArg1",
                        e.getMessage());
    }

    @Test
    public void testUndefinedTableArgsInTableSql() {
        Table mainTable = mainTableBuilder
                        .sql("SELECT something {{$$table.args.mainArg1}} blah {{$$table.args.mainArg2}} FROM")
                        .build();

        TableArgumentValidator argValidator = new TableArgumentValidator(elideTableConfig, dictionary, mainTable);
        Exception e = assertThrows(IllegalStateException.class, () -> argValidator.validate());
        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'mainArg2' is not defined but found '{{$$table.args.mainArg2}}' in table's sql.",
                        e.getMessage());
    }

    @Test
    public void testUndefinedTableArgsInColumnDefinition() {
        Table mainTable = mainTableBuilder.build();
        TableArgumentValidator argValidator = new TableArgumentValidator(elideTableConfig, dictionary, mainTable);
        Exception e = assertThrows(IllegalStateException.class, () -> argValidator.validate());
        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'mainArg2' is not defined but found '{{$$table.args.mainArg2}}' in column's definition.",
                        e.getMessage());
    }

    @Test
    public void testUndefinedRequiredTableArgsForJoinTablesCaseMissing() {
        // Add argument 'mainArg2' so that column definition passes.
        Table mainTable = mainTableBuilder
                        .argument(Argument.builder()
                                        .name("mainArg2")
                                        .type(Type.TEXT)
                                        .build())
                        .build();

        TableArgumentValidator argValidator = new TableArgumentValidator(elideTableConfig, dictionary, mainTable);
        Exception e = assertThrows(IllegalStateException.class, () -> argValidator.validate());
        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'joinArg1' with type 'INTEGER' is not defined but is required by join table: namespace_JoinTable.",
                        e.getMessage());
    }

    @Test
    public void testUndefinedRequiredTableArgsForJoinTablesCasetypeMismatch() {
        // Add argument 'mainArg2' so that column definition passes.
        // Add argument 'joinArg1' with different type.
        Table mainTable = mainTableBuilder
                        .argument(Argument.builder()
                                        .name("mainArg2")
                                        .type(Type.TEXT)
                                        .build())
                        .argument(Argument.builder()
                                        .name("joinArg1")
                                        .type(Type.TEXT)
                                        .build())
                        .build();

        TableArgumentValidator argValidator = new TableArgumentValidator(elideTableConfig, dictionary, mainTable);
        Exception e = assertThrows(IllegalStateException.class, () -> argValidator.validate());
        assertEquals("Failed to verify table arguments for table: namespace_MainTable. Argument 'joinArg1' with type 'INTEGER' is not defined but is required by join table: namespace_JoinTable.",
                        e.getMessage());
    }

    @Test
    public void testInvalidDefaultValue() {
        Table newTable = Table.builder()
                        .name("NewTable")
                        .namespace("namespace")
                        .argument(Argument.builder()
                                        .name("testArg")
                                        .type(Type.INTEGER)
                                        .values(new HashSet<String>(Arrays.asList("1" , "2", "3")))
                                        .defaultValue("4")
                                        .build())
                        .build();

        TableArgumentValidator argValidator = new TableArgumentValidator(elideTableConfig, dictionary, newTable);
        Exception e = assertThrows(IllegalStateException.class, () -> argValidator.validate());
        assertEquals("Failed to verify table arguments for table: namespace_NewTable. Default Value for argument 'testArg' must match one of these values: [1, 2, 3]. Found '4' instead.",
                        e.getMessage());
    }

    @Test
    public void testInvalidtableSource() {
        Table newTable = Table.builder()
                        .name("NewTable")
                        .namespace("namespace")
                        .argument(Argument.builder()
                                        .name("testArg")
                                        .type(Type.INTEGER)
                                        .defaultValue("4")
                                        .tableSource(TableSource.builder()
                                                        .namespace("namespace")
                                                        .table("JoinTable")
                                                        .column("columnA")
                                                        .build())
                                        .build())
                        .build();

        Set<Table> tables = new HashSet<>();
        tables.addAll(this.tables);
        tables.add(newTable);
        ElideTableConfig elideTableConfig = new ElideTableConfig(tables);
        TableArgumentValidator argValidator = new TableArgumentValidator(elideTableConfig, dictionary, newTable);
        Exception e = assertThrows(IllegalStateException.class, () -> argValidator.validate());
        assertEquals("Invalid tableSource : TableSource(table=JoinTable, namespace=namespace, column=columnA, suggestionColumns=null) . Field : columnA is undefined for hjson model: JoinTable", e.getMessage());
    }
}
