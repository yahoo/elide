/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest.replaceDynamicAliases;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;

import org.junit.jupiter.api.Test;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.Id;
import javax.sql.DataSource;

public class JoinExpressionExtractorTest {

    private static final String ALIAS_PREFIX = "com_yahoo_elide_datastores_aggregation_queryengines_sql_expression_";
    private static final Pattern REPEATEDSPACE_PATTERN = Pattern.compile("\\s\\s*");
    private static final String NL = System.lineSeparator();

    private SQLTable table;
    private MetaDataStore metaDataStore;
    private SQLQueryEngine engine;

    public JoinExpressionExtractorTest() {

        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(MainTable.class));
        models.add(ClassType.of(JoinTable.class));
        models.add(ClassType.of(JoinJoinTable.class));

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        models.stream().forEach(dictionary::bindEntity);

        metaDataStore = new MetaDataStore(models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        // The query engine populates the metadata store with actual tables.
        engine = new SQLQueryEngine(metaDataStore, new ConnectionDetails(mockDataSource, new H2Dialect()));
        table = metaDataStore.getTable(ClassType.of(MainTable.class));
    }

    // Case:
    // dim1 -> {{join.dim1}}
    // {{join.dim1}} -> Physical
    @Test
    void test1() {

        SQLDimensionProjection dim1 = (SQLDimensionProjection) table.getDimensionProjection("dim1");

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim1)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join_XXX`.`dim1` AS `dim1` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join_XXX` " + NL
                        + "ON " + NL
                        + "  `MainTable`.`id` = `MainTable_join_XXX`.`id` ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Case:
    // dim -> {{join.dim2}}
    // {{join.dim2}} -> {{join.dim3}}
    // {{join.dim3}} -> {{joinjoin.dim3}}
    // {{joinjoin.dim3}} -> Physical
    @Test
    void test2() {

        SQLDimensionProjection dim2 = (SQLDimensionProjection) table.getDimensionProjection("dim2");

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim2)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join_XXX_joinjoin_XXX`.`dim3` AS `dim2` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join_XXX` " + NL
                        + "ON " + NL
                        + "  `MainTable`.`id` = `MainTable_join_XXX`.`id` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `joinjoin_table` AS `MainTable_join_XXX_joinjoin_XXX` " + NL
                        + "ON " + NL
                        + "  `MainTable_join_XXX`.`id` = `MainTable_join_XXX_joinjoin_XXX`.`id` ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Case:
    // dim3 -> {{join.joinjoin.dim2}}
    // {{join.joinjoin.dim2}} -> {{join.joinjoin.dim3}}
    // {{join.joinjoin.dim3}} -> Physical
    @Test
    void test3() {

        SQLDimensionProjection dim3 = (SQLDimensionProjection) table.getDimensionProjection("dim3");

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim3)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join_XXX_joinjoin_XXX`.`dim3` AS `dim3` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join_XXX` " + NL
                        + "ON " + NL
                        + "  `MainTable`.`id` = `MainTable_join_XXX`.`id` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `joinjoin_table` AS `MainTable_join_XXX_joinjoin_XXX` " + NL
                        + "ON " + NL
                        + "  `MainTable_join_XXX`.`id` = `MainTable_join_XXX_joinjoin_XXX`.`id` ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Both dim4 and dim5 passes same value for join expression's argument 'exprArg'
    // Single Join expression is generated.
    @Test
    void test4a() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        SQLDimensionProjection dim4 = (SQLDimensionProjection) table.getDimensionProjection("dim4", "dim4", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        SQLDimensionProjection dim5 = (SQLDimensionProjection) table.getDimensionProjection("dim5", "dim5", dim5Arg);

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim4)
                        .dimensionProjection(dim5)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2_XXX`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_XXX`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_XXX` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_XXX`.`dim4` - 'foo' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // dim4 and dim5 passes different value for join expression's argument 'exprArg'
    // 2 Join expressions are generated.
    @Test
    void test4b() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("value4").build());
        SQLDimensionProjection dim4 = (SQLDimensionProjection) table.getDimensionProjection("dim4", "dim4", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("value5").build());
        SQLDimensionProjection dim5 = (SQLDimensionProjection) table.getDimensionProjection("dim5", "dim5", dim5Arg);

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim4)
                        .dimensionProjection(dim5)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);
        System.out.println(generatedSql);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2_XXX`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_XXX`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_XXX` " + NL
                        + "ON " + NL
                        + "  value = 'value4' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_XXX`.`dim4` - 'foo' "
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_XXX` " + NL
                        + "ON " + NL
                        + "  value = 'value5' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_XXX`.`dim4` - 'foo' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Both dim4 and dim5 passes same value for join expression's argument 'exprArg'
    // dim4 and dim5 passes different value for join table's column argument 'joinArg4'
    // 2 Join expressions are generated.
    @Test
    void test4c() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim4Arg.put("joinArg4", Argument.builder().name("joinArg4").value("foo4").build());
        SQLDimensionProjection dim4 = (SQLDimensionProjection) table.getDimensionProjection("dim4", "dim4", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim5Arg.put("joinArg4", Argument.builder().name("joinArg4").value("foo5").build());
        SQLDimensionProjection dim5 = (SQLDimensionProjection) table.getDimensionProjection("dim5", "dim5", dim5Arg);

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim4)
                        .dimensionProjection(dim5)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2_XXX`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_XXX`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_XXX` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_XXX`.`dim4` - 'foo4' "
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_XXX` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_XXX`.`dim4` - 'foo5' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Both dim4 and dim5 passes same value for join expression's argument 'exprArg'
    // dim4 and dim5 passes different value for column argument 'arg6'
    // 2 Join expressions are generated.
    @Test
    void test4d() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim4Arg.put("arg6", Argument.builder().name("arg6").value("bar4").build());
        SQLDimensionProjection dim4 = (SQLDimensionProjection) table.getDimensionProjection("dim4", "dim4", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim5Arg.put("arg6", Argument.builder().name("arg6").value("bar5").build());
        SQLDimensionProjection dim5 = (SQLDimensionProjection) table.getDimensionProjection("dim5", "dim5", dim5Arg);

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim4)
                        .dimensionProjection(dim5)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2_XXX`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_XXX`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_XXX` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar4' = `MainTable_join2_XXX`.`dim4` - 'foo' "
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_XXX` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar5' = `MainTable_join2_XXX`.`dim4` - 'foo' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    private String formatExpected(String expectedSQL) {
        // Remove spaces at the start of each line with in string
        expectedSQL = expectedSQL.replaceAll("(?m)^\\s*", "");
        expectedSQL = expectedSQL.replace(NL, "");
        return REPEATEDSPACE_PATTERN.matcher(expectedSQL).replaceAll(" ");
    }

    private String formatGenerated(String generatedSql) {
        generatedSql = generatedSql.replace(ALIAS_PREFIX, "");
        generatedSql = REPEATEDSPACE_PATTERN.matcher(generatedSql).replaceAll(" ");
        return replaceDynamicAliases(generatedSql);
    }
}


@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "main_table")
@Include(name = "mainTable")
class MainTable {

    @Id
    private String id;

    @DimensionFormula("{{join.dim1}}")
    private String dim1;

    @DimensionFormula("{{join.dim2}}")
    private String dim2;

    @DimensionFormula("{{join.joinjoin.dim2}}")
    private String dim3;

    @Join("{{$id}} = {{join.$id}}")
    private JoinTable join;

    @DimensionFormula(value = "{{join2.dim1}}",
                      arguments = { @ArgumentDefinition(name = "exprArg", type = ValueType.TEXT),
                                    @ArgumentDefinition(name = "joinArg4", type = ValueType.TEXT)})
    private String dim4;

    @DimensionFormula(value = "{{join2.dim1}}",
                      arguments = { @ArgumentDefinition(name = "exprArg", type = ValueType.TEXT),
                                    @ArgumentDefinition(name = "joinArg4", type = ValueType.TEXT)})
    private String dim5;

    @Join("value = '{{$$column.args.exprArg}}' AND {{dim6}} = {{join2.dim4}}")
    private JoinTable join2;

    @DimensionFormula(value = "{{$dim6}} - '{{$$column.args.arg6}}'",
                      arguments = {@ArgumentDefinition(name = "arg6", type = ValueType.TEXT, defaultValue = "bar")})
    private String dim6;
}


@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "join_table")
@Include(name = "joinTable")
class JoinTable {

    @Id
    private String id;

    @DimensionFormula("{{$dim1}}")
    private String dim1;

    @DimensionFormula("{{dim3}}")
    private String dim2;

    @DimensionFormula("{{joinjoin.dim3}}")
    private String dim3;

    @DimensionFormula(value = "{{$dim4}} - '{{$$column.args.joinArg4}}'",
                      arguments = {@ArgumentDefinition(name = "joinArg4", type = ValueType.TEXT, defaultValue = "foo")})
    private String dim4;

    @Join("{{$id}} = {{joinjoin.$id}}")
    private JoinJoinTable joinjoin;
}


@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "joinjoin_table")
@Include(name = "joinjoinTable")
class JoinJoinTable {

    @Id
    private String id;

    @DimensionFormula("{{dim2}}")
    private String dim1;

    @DimensionFormula("{{dim3}}")
    private String dim2;

    @DimensionFormula("{{$dim3}}")
    private String dim3;
}