/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

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
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
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

import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.sql.DataSource;

public class JoinExpressionExtractorTest {

    private static final String ALIAS_PREFIX = "com_yahoo_elide_datastores_aggregation_queryengines_sql_expression_";
    private static final Pattern REPEATEDSPACE_PATTERN = Pattern.compile("\\s\\s*");
    private static final String NL = System.lineSeparator();

    private SQLTable table;
    private MetaDataStore metaDataStore;
    private SQLQueryEngine engine;
    private Map<String, Argument> queryArgs;

    public JoinExpressionExtractorTest() {

        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(MainTable.class));
        models.add(ClassType.of(JoinTable.class));
        models.add(ClassType.of(JoinJoinTable.class));

        EntityDictionary dictionary = EntityDictionary.builder().build();

        models.stream().forEach(dictionary::bindEntity);

        metaDataStore = new MetaDataStore(dictionary.getScanner(), models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        // The query engine populates the metadata store with actual tables.
        engine = new SQLQueryEngine(metaDataStore, (unused) -> new ConnectionDetails(mockDataSource, new H2Dialect()));
        table = metaDataStore.getTable(ClassType.of(MainTable.class));
        queryArgs = new HashMap<>();
        queryArgs.put("tableArg", Argument.builder().name("tableArg").value("tableArgValue").build());
    }

    // Case:
    // dim1 -> {{join.dim1}}
    // {{join.dim1}} -> Physical
    @Test
    void test2TableJoin() {

        SQLDimensionProjection dim1 = (SQLDimensionProjection) table.getDimensionProjection("dim1");

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim1)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join_258525107`.`dim1` AS `dim1` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join_258525107` " + NL
                        + "ON " + NL
                        + "  `MainTable`.`id` = `MainTable_join_258525107`.`id` ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Case:
    // dim -> {{join.dim2}}
    // {{join.dim2}} -> {{join.dim3}}
    // {{join.dim3}} -> {{joinjoin.dim3}}
    // {{joinjoin.dim3}} -> Physical
    @Test
    void test3TableJoinWithLogicalColumn() {

        SQLDimensionProjection dim2 = (SQLDimensionProjection) table.getDimensionProjection("dim2");

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim2)
                        .arguments(queryArgs)
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join_258525107_joinjoin_88940112`.`dim3` - 'tableArgValue' AS `dim2` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join_258525107` " + NL
                        + "ON " + NL
                        + "  `MainTable`.`id` = `MainTable_join_258525107`.`id` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `joinjoin_table` AS `MainTable_join_258525107_joinjoin_88940112` " + NL
                        + "ON " + NL
                        + "  `MainTable_join_258525107`.`id` = `MainTable_join_258525107_joinjoin_88940112`.`id` ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Case:
    // dim3 -> {{join.joinjoin.dim2}}
    // {{join.joinjoin.dim2}} -> {{join.joinjoin.dim3}}
    // {{join.joinjoin.dim3}} -> Physical
    @Test
    void test3TableJoinWithoutLogicalColumn() {

        SQLDimensionProjection dim3 = (SQLDimensionProjection) table.getDimensionProjection("dim3");

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim3)
                        .arguments(queryArgs)
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join_258525107_joinjoin_88940112`.`dim3` - 'tableArgValue' AS `dim3` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join_258525107` " + NL
                        + "ON " + NL
                        + "  `MainTable`.`id` = `MainTable_join_258525107`.`id` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `joinjoin_table` AS `MainTable_join_258525107_joinjoin_88940112` " + NL
                        + "ON " + NL
                        + "  `MainTable_join_258525107`.`id` = `MainTable_join_258525107_joinjoin_88940112`.`id` ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Both dim4 and dim5 passes same value for join expression's argument 'exprArg'
    // Single Join expression is generated.
    @Test
    void testArgumentsInJoinExprCase1() {

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
                        + "  DISTINCT `MainTable_join2_201179280`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_201179280`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_201179280` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_201179280`.`dim4` - 'foo' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // dim4 and dim5 passes different value for join expression's argument 'exprArg'
    // 2 Join expressions are generated.
    @Test
    void testArgumentsInJoinExprCase2a() {

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

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2_63993339`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_86115708`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_63993339` " + NL
                        + "ON " + NL
                        + "  value = 'value4' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_63993339`.`dim4` - 'foo' "
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_86115708` " + NL
                        + "ON " + NL
                        + "  value = 'value5' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_86115708`.`dim4` - 'foo' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // dim4x and dim5x passes different value for join expression's argument 'exprArg'
    // 1 Join expression is generated as they invoke dim4 and dim5 using sql helper with same value
    @Test
    void testArgumentsInJoinExprCase2b() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("value4").build());
        SQLDimensionProjection dim4x = (SQLDimensionProjection) table.getDimensionProjection("dim4x", "dim4x", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("value5").build());
        SQLDimensionProjection dim5x = (SQLDimensionProjection) table.getDimensionProjection("dim5x", "dim5x", dim5Arg);

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim4x)
                        .dimensionProjection(dim5x)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2_156385021`.`dim1` - 'value4' AS `dim4x`," + NL
                        + "           `MainTable_join2_156385021`.`dim1` - 'value5' AS `dim5x` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_156385021` " + NL
                        + "ON " + NL
                        + "  value = 'fixedExpr' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_156385021`.`dim4` - 'fixedArg' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Both dim4 and dim5 passes same value for join expression's argument 'exprArg'
    // dim4 and dim5 passes different value for join table's column argument 'joinArg'
    // 2 Join expressions are generated.
    @Test
    void testArgumentsInJoinExprCase3() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim4Arg.put("joinArg", Argument.builder().name("joinArg").value("foo4").build());
        SQLDimensionProjection dim4 = (SQLDimensionProjection) table.getDimensionProjection("dim4", "dim4", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim5Arg.put("joinArg", Argument.builder().name("joinArg").value("foo5").build());
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
                        + "  DISTINCT `MainTable_join2_201179732`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_201179856`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_201179732` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_201179732`.`dim4` - 'foo4' "
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_201179856` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2_201179856`.`dim4` - 'foo5' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Both dim4 and dim5 passes same value for join expression's argument 'exprArg'
    // dim4 and dim5 passes different value for column argument 'arg6'
    // 2 Join expressions are generated.
    @Test
    void testArgumentsInJoinExprCase4() {

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
                        + "  DISTINCT `MainTable_join2_148518185`.`dim1` AS `dim4`," + NL
                        + "           `MainTable_join2_209848490`.`dim1` AS `dim5` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_148518185` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar4' = `MainTable_join2_148518185`.`dim4` - 'foo' "
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2_209848490` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar5' = `MainTable_join2_209848490`.`dim4` - 'foo' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // Both dim4a and dim5a passes same value for join expression's argument 'exprArg'
    // dim4a and dim5a passes different value for join table's column argument 'joinArg'
    // 1 Join expressions is generated as fixed value of joinArg is used while calling join table's column.
    @Test
    void testArgumentsInJoinExprCase5a() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim4Arg.put("joinArg", Argument.builder().name("joinArg").value("foo4").build());
        SQLDimensionProjection dim4a = (SQLDimensionProjection) table.getDimensionProjection("dim4a", "dim4a", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("same").build());
        dim5Arg.put("joinArg", Argument.builder().name("joinArg").value("foo5").build());
        SQLDimensionProjection dim5a = (SQLDimensionProjection) table.getDimensionProjection("dim5a", "dim5a", dim5Arg);

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim4a)
                        .dimensionProjection(dim5a)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2a_157775546`.`dim1` AS `dim4a`," + NL
                        + "           `MainTable_join2a_157775546`.`dim1` AS `dim5a` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2a_157775546` " + NL
                        + "ON " + NL
                        + "  value = 'same' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2a_157775546`.`dim4` - 'fixedFoo' ";

        assertEquals(formatExpected(expectedSQL), formatGenerated(generatedSql));
    }

    // dim4a and dim5a passes different value for join expression's argument 'exprArg'
    // dim4a and dim5a passes different value for join table's column argument 'joinArg'
    // 2 Join expressions are generated as 'exprArg' is different but fixed value of 'joinArg' is used.
    @Test
    void testArgumentsInJoinExprCase5b() {

        Map<String, Argument> dim4Arg = new HashMap<>();
        dim4Arg.put("exprArg", Argument.builder().name("exprArg").value("value4").build());
        dim4Arg.put("joinArg", Argument.builder().name("joinArg").value("foo4").build());
        SQLDimensionProjection dim4a = (SQLDimensionProjection) table.getDimensionProjection("dim4a", "dim4a", dim4Arg);

        Map<String, Argument> dim5Arg = new HashMap<>();
        dim5Arg.put("exprArg", Argument.builder().name("exprArg").value("value5").build());
        dim5Arg.put("joinArg", Argument.builder().name("joinArg").value("foo5").build());
        SQLDimensionProjection dim5a = (SQLDimensionProjection) table.getDimensionProjection("dim5a", "dim5a", dim5Arg);

        Query query = Query.builder()
                        .source(table)
                        .dimensionProjection(dim4a)
                        .dimensionProjection(dim5a)
                        .arguments(emptyMap())
                        .build();

        String generatedSql = engine.explain(query).get(0);

        String expectedSQL =
                          "SELECT " + NL
                        + "  DISTINCT `MainTable_join2a_16142728`.`dim1` AS `dim4a`," + NL
                        + "           `MainTable_join2a_29675529`.`dim1` AS `dim5a` " + NL
                        + "FROM " + NL
                        + "  `main_table` AS `MainTable` " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2a_16142728` " + NL
                        + "ON " + NL
                        + "  value = 'value4' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2a_16142728`.`dim4` - 'fixedFoo' " + NL
                        + "LEFT OUTER JOIN " + NL
                        + "  `join_table` AS `MainTable_join2a_29675529` " + NL
                        + "ON " + NL
                        + "  value = 'value5' AND `MainTable`.`dim6` - 'bar' = `MainTable_join2a_29675529`.`dim4` - 'fixedFoo' ";

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
        return REPEATEDSPACE_PATTERN.matcher(generatedSql).replaceAll(" ");
    }
}


@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "main_table")
@Include(name = "mainTable")
@TableMeta(arguments = {@ArgumentDefinition(name = "tableArg", type = ValueType.TEXT)})
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
                                    @ArgumentDefinition(name = "joinArg", type = ValueType.TEXT)})
    private String dim4;

    @DimensionFormula(value = "{{join2.dim1}}",
                      arguments = { @ArgumentDefinition(name = "exprArg", type = ValueType.TEXT),
                                    @ArgumentDefinition(name = "joinArg", type = ValueType.TEXT)})
    private String dim5;

    @DimensionFormula(value = "{{join2a.dim1}}",
                      arguments = { @ArgumentDefinition(name = "exprArg", type = ValueType.TEXT),
                                    @ArgumentDefinition(name = "joinArg", type = ValueType.TEXT)})
    private String dim4a;

    @DimensionFormula(value = "{{join2a.dim1}}",
                      arguments = { @ArgumentDefinition(name = "exprArg", type = ValueType.TEXT),
                                    @ArgumentDefinition(name = "joinArg", type = ValueType.TEXT)})
    private String dim5a;

    @DimensionFormula(value = "{{sql column='dim4[exprArg:fixedExpr][joinArg:fixedArg]'}} - '{{$$column.args.exprArg}}'",
                      arguments = { @ArgumentDefinition(name = "exprArg", type = ValueType.TEXT),
                                    @ArgumentDefinition(name = "joinArg", type = ValueType.TEXT)})
    private String dim4x;

    @DimensionFormula(value = "{{sql column='dim5[exprArg:fixedExpr][joinArg:fixedArg]'}} - '{{$$column.args.exprArg}}'",
                      arguments = { @ArgumentDefinition(name = "exprArg", type = ValueType.TEXT),
                                    @ArgumentDefinition(name = "joinArg", type = ValueType.TEXT)})
    private String dim5x;

    @Join("value = '{{$$column.args.exprArg}}' AND {{dim6}} = {{join2.dim4}}")
    private JoinTable join2;

    @Join("value = '{{$$column.args.exprArg}}' AND {{dim6}} = {{join2a.dim4a}}")
    private JoinTable join2a;

    @DimensionFormula(value = "{{$dim6}} - '{{$$column.args.arg6}}'",
                      arguments = {@ArgumentDefinition(name = "arg6", type = ValueType.TEXT, defaultValue = "bar")})
    private String dim6;
}


@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "join_table")
@Include(name = "joinTable")
@TableMeta(arguments = {@ArgumentDefinition(name = "tableArg", type = ValueType.TEXT)})
class JoinTable {

    @Id
    private String id;

    @DimensionFormula("{{$dim1}}")
    private String dim1;

    @DimensionFormula("{{dim3}}")
    private String dim2;

    @DimensionFormula("{{joinjoin.dim3}}")
    private String dim3;

    @DimensionFormula(value = "{{$dim4}} - '{{$$column.args.joinArg}}'",
                      arguments = {@ArgumentDefinition(name = "joinArg", type = ValueType.TEXT, defaultValue = "foo")})
    private String dim4;

    @DimensionFormula(value = "{{sql column='dim4[joinArg:fixedFoo]'}}",
                      arguments = {@ArgumentDefinition(name = "joinArg", type = ValueType.TEXT, defaultValue = "foo")})
    private String dim4a;

    @Join("{{$id}} = {{joinjoin.$id}}")
    private JoinJoinTable joinjoin;
}


@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "joinjoin_table")
@Include(name = "joinjoinTable")
@TableMeta(arguments = {@ArgumentDefinition(name = "tableArg", type = ValueType.TEXT)})
class JoinJoinTable {

    @Id
    private String id;

    @DimensionFormula("{{dim2}}")
    private String dim1;

    @DimensionFormula("{{dim3}}")
    private String dim2;

    @DimensionFormula("{{$dim3}} - '{{$$table.args.tableArg}}'")
    private String dim3;
}
