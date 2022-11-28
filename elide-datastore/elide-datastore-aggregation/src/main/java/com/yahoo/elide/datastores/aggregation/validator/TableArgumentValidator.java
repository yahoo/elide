/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.validator;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable.hasSql;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable.resolveTableOrSubselect;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.TableArgReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * Verifies all defined arguments for table.
 * Ensures all arguments required for table are defined.
 * For any table:
 * <ul>
 *   <li>All the table arguments used (in the column definition, Join expressions, or table's SQL) must be defined
 *    in the argument list for this table (with or without default value).</li>
 *   <li>All the table arguments required for join tables must be defined in the argument list for this table
 *    (with or without default value).</li>
 * </ul>
 */
public class TableArgumentValidator {

    private final MetaDataStore metaDataStore;
    private final SQLTable table;
    private final String errorMsgPrefix;
    private final ExpressionParser parser;
    private final EntityDictionary dictionary;

    public TableArgumentValidator(MetaDataStore metaDataStore, SQLTable table) {
        this.metaDataStore = metaDataStore;
        this.table = table;
        this.errorMsgPrefix = String.format("Failed to verify table arguments for table: %s. ", table.getName());
        this.parser = new ExpressionParser(metaDataStore);
        this.dictionary = metaDataStore.getMetadataDictionary();
    }

    public void validate() {

        table.getArgumentDefinitions().forEach(arg -> {
            verifyValues(arg, errorMsgPrefix);
            verifyDefaultValue(arg, errorMsgPrefix);
        });

        verifyTableArgsInTableSql();
        verifyTableArgsInColumnDefinition();
        verifyTableArgsInJoinExpressions();
        verifyRequiredTableArgsForJoinTables();
    }

    private void verifyTableArgsInTableSql() {
        Type<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        Query mockQuery =  Query.builder()
                .source(table)
                .dimensionProjections(table.getDimensionProjections())
                .metricProjections(table.getMetricProjections())
                .timeDimensionProjections(table.getTimeDimensionProjections())
                .build();

        if (hasSql(tableClass)) {
            String selectSql = resolveTableOrSubselect(dictionary, tableClass, mockQuery);
            verifyTableArgsExists(selectSql, "in table's sql.");
        }
    }

    private void verifyTableArgsInColumnDefinition() {
        table.getColumnProjections().forEach(column -> verifyTableArgsExists(column.getExpression(),
                        String.format("in definition of column: '%s'.", column.getName())));
    }

    private void verifyTableArgsInJoinExpressions() {
        table.getJoins().forEach((joinName, join) -> verifyTableArgsExists(join.getJoinExpression(),
                        String.format("in definition of join: '%s'.", joinName)));
    }

    private void verifyTableArgsExists(String expression, String errorMsgSuffix) {
        parser.parse(table, expression).stream()
                        .filter(ref -> ref instanceof TableArgReference)
                        .map(TableArgReference.class::cast)
                        .map(TableArgReference::getArgName)
                        .forEach(argName -> {
                            if (!table.hasArgumentDefinition(argName) && !hasTemplateFilterArgument(argName)) {
                                throw new IllegalStateException(String.format(errorMsgPrefix
                                                + "Argument '%s' is not defined but found '{{$$table.args.%s}}' "
                                                + errorMsgSuffix, argName, argName));
                            }
                        });
    }

    private boolean hasTemplateFilterArgument(String argName) {
        return table.getRequiredFilter() != null && table.getRequiredFilter().contains("{{" + argName + "}}");
    }

    private void verifyRequiredTableArgsForJoinTables() {
        table.getJoins().forEach((joinName, sqlJoin) -> {
            SQLTable joinTable = metaDataStore.getTable(sqlJoin.getJoinTableType());
            joinTable.getArgumentDefinitions().forEach(joinArgDef -> {
                String joinArgName = joinArgDef.getName();

                if (table.hasArgumentDefinition(joinArgName)) {
                    if (joinArgDef.getType() != table.getArgumentDefinition(joinArgName).getType()) {
                        throw new IllegalStateException(String.format(errorMsgPrefix
                                        + "Argument type mismatch. Join table: '%s' has same Argument: '%s'"
                                        + " with type '%s'.",
                                        joinTable.getName(), joinArgName, joinArgDef.getType()));
                    }
                } else if (StringUtils.isBlank(joinArgDef.getDefaultValue().toString())) {
                    throw new IllegalStateException(String.format(errorMsgPrefix
                                    + "Argument '%s' with type '%s' is not defined but is required by join table: %s.",
                                    joinArgName, joinArgDef.getType(), joinTable.getName()));
                }
            });
        });
    }

    public static void verifyValues(ArgumentDefinition argument, String errorMsgPrefix) {
        Set<String> values = argument.getValues();

        if (CollectionUtils.isEmpty(values)) {
            return;
        }

        values.forEach(value -> {
            if (!argument.getType().matches(value)) {
                throw new IllegalStateException(String.format(errorMsgPrefix
                                + "Value: '%s' for Argument '%s' with Type '%s' is invalid.",
                                value, argument.getName(), argument.getType()));
            }
        });
    }

    public static void verifyDefaultValue(ArgumentDefinition argument, String errorMsgPrefix) {
        Object value = argument.getDefaultValue();

        /*
         * Arguments must have default values or else we won't be able to evaluate the correctness of their expressions
         * at build time.
         */
        if (value == null) {
            throw new IllegalStateException(String.format("Argument '%s' is missing a default value",
                    argument.getName()));
        }

        String defaultValue = value.toString();
        verifyValue(argument, defaultValue, errorMsgPrefix + "Default ");
    }

    public static void verifyValue(ArgumentDefinition argument, String value, String errorMsgPrefix) {

        Set<String> values = argument.getValues();

        if (StringUtils.isBlank(value)) {
            return;
        }

        if (CollectionUtils.isEmpty(values)) {
            if (!argument.getType().matches(value)) {
                throw new IllegalStateException(String.format(errorMsgPrefix
                                + "Value: '%s' for Argument '%s' with Type '%s' is invalid.",
                                value, argument.getName(), argument.getType()));
            }
        } else {
            if (!values.contains(value)) {
                throw new IllegalStateException(String.format(errorMsgPrefix
                                + "Value: '%s' for Argument '%s' with Type '%s' must match one of these values: %s.",
                                value, argument.getName(), argument.getType(), values));
            }
        }
    }
}
