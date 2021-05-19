/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.validator;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.hasSql;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.resolveTableOrSubselect;
import static com.yahoo.elide.modelconfig.validator.DynamicConfigValidator.validateNameUniqueness;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.TableArgReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import org.apache.commons.collections.CollectionUtils;

import java.util.Set;

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

        validateNameUniqueness(table.getArgumentDefinitions(),
                        errorMsgPrefix + "Multiple Table Arguments found with the same name: ");

        verifyTableArgsInTableSql();
        verifyTableArgsInColumnDefinition();
        verifyTableArgsInJoinExpressions();
        verifyRequiredTableArgsForJoinTables();

        table.getArgumentDefinitions().forEach(arg -> {
            verifyValues(arg, errorMsgPrefix);
            verifyDefaultvalue(arg, errorMsgPrefix);
        });
    }

    private void verifyTableArgsInTableSql() {
        Type<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        if (hasSql(tableClass)) {
            String selectSql = resolveTableOrSubselect(dictionary, tableClass);
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

    private void verifyTableArgsExists(String expression, String errorMsg) {
        parser.parse(table, expression).stream()
                        .filter(ref -> ref instanceof TableArgReference)
                        .map(TableArgReference.class::cast)
                        .map(TableArgReference::getArgName)
                        .forEach(argName -> {
                            if (!table.hasArgumentDefinition(argName)) {
                                throw new IllegalStateException(String.format(errorMsgPrefix
                                                + "Argument '%s' is not defined but found '{{$$table.args.%s}}' "
                                                + errorMsg, argName, argName));
                            }
                        });
    }

    private void verifyRequiredTableArgsForJoinTables() {
        table.getJoins().forEach((joinName, sqlJoin) -> {
            SQLTable joinTable = metaDataStore.getTable(sqlJoin.getJoinTableType());
            joinTable.getArgumentDefinitions().forEach(argDef -> {
                String argName = argDef.getName();
                ValueType argType = argDef.getType();
                if (!(table.hasArgumentDefinition(argName)
                                && argType == table.getArgumentDefinition(argName).getType())) {
                    throw new IllegalStateException(String.format(
                                    errorMsgPrefix + "Argument '%s' with type '%s' is not defined but is required by"
                                                    + " join table: %s.",
                                    argName, argType, joinTable.getName()));
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

    public static void verifyDefaultvalue(ArgumentDefinition argument, String errorMsgPrefix) {

        if (argument.getDefaultValue() == null) {
            return;
        }

        String defaultValue = argument.getDefaultValue().toString();
        Set<String> values = argument.getValues();

        if (CollectionUtils.isEmpty(values)) {
            if (!argument.getType().matches(defaultValue)) {
                throw new IllegalStateException(String.format(errorMsgPrefix
                                + "Default Value: '%s' for Argument '%s' with Type '%s' is invalid.",
                                defaultValue, argument.getName(), argument.getType()));
            }
        } else {
            if (!values.contains(defaultValue)) {
                throw new IllegalStateException(String.format(errorMsgPrefix
                                + "Default Value: '%s' for Argument '%s' with Type '%s' must match one of these"
                                + " values: %s.",
                                defaultValue, argument.getName(), argument.getType(), values));
            }
        }
    }
}
