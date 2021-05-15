/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.validator;

import static com.yahoo.elide.modelconfig.validator.DynamicConfigValidator.validateNameUniqueness;
import static com.yahoo.elide.modelconfig.validator.DynamicConfigValidator.validateTableSource;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.modelconfig.model.Argument;
import com.yahoo.elide.modelconfig.model.Column;
import com.yahoo.elide.modelconfig.model.ElideTableConfig;
import com.yahoo.elide.modelconfig.model.Table;
import com.yahoo.elide.modelconfig.model.Type;
import com.yahoo.elide.modelconfig.model.reference.HandlebarReferenceParser;
import com.yahoo.elide.modelconfig.model.reference.TableArgReference;

import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Set;

/**
 * Verifies all defined arguments for table.
 * Ensures all arguments required for table are defined.
 */
public class TableArgumentValidator {

    private final ElideTableConfig elideTableConfig;
    private final EntityDictionary dictionary;
    private final Table table;
    private final String errorMsgPrefix;

    public TableArgumentValidator(ElideTableConfig elideTableConfig, EntityDictionary dictionary, Table table) {
        this.elideTableConfig = elideTableConfig;
        this.dictionary = dictionary;
        this.table = table;
        this.errorMsgPrefix = String.format("Failed to verify table arguments for table: %s. ", table.getGlobalName());
    }

    public void validate() {
        validateNameUniqueness(table.getArguments(), String.format("For table: %s, Multiple Table Arguments found with"
                                                                   + " the same name: ", table.getGlobalName()));
        verifyTableArgsInTableSql();
        verifyTableArgsInColumnDefinition();
        verifyRequiredTableArgsForJoinTables();

        table.getArguments().forEach(arg -> {
            verifyDefaultValue(arg, errorMsgPrefix);
            validateTableSource(elideTableConfig, dictionary, arg.getTableSource());
        });
    }

    /**
     * Verify all the table arguments used by table's sql are defined.
     * @throws IllegalStateException if validation fails.
     */
    private void verifyTableArgsInTableSql() {

        new HandlebarReferenceParser().parse(table.getSql()).forEach(ref -> {
            if (ref instanceof TableArgReference) {
                String argName = ((TableArgReference) ref).getArgName();
                if (!table.hasArgument(argName)) {
                    throw new IllegalStateException(String.format(errorMsgPrefix
                                    + "Argument '%s' is not defined but found '{{$$table.args.%s}}' in table's sql.",
                                    argName, argName));
                }
            }
        });
    }

    /**
     * Verify all the table arguments used by definitions of measure, dimension and join are defined.
     * @throws IllegalStateException if validation fails.
     */
    private void verifyTableArgsInColumnDefinition() {

        // Get the list of all table arguments used in dimension/measure/join's definition.
        table.getColumns().stream()
                        .map(Column::getHandlebarReferences)
                        .flatMap(Collection::stream)
                        .filter(ref -> ref instanceof TableArgReference)
                        .map(TableArgReference.class::cast)
                        .map(TableArgReference::getArgName)
                        .forEach(argName -> {
                            if (!table.hasArgument(argName)) {
                                throw new IllegalStateException(String.format(errorMsgPrefix
                                                + "Argument '%s' is not defined but found '{{$$table.args.%s}}' in"
                                                + " column's definition.",
                                                argName, argName));
                            }
                        });
    }

    /**
     * Verify all the required table arguments for join table are defined.
     * @throws IllegalStateException if validation fails.
     */
    private void verifyRequiredTableArgsForJoinTables() {

        table.getJoins().forEach(join -> {
            String joinModelName = join.getTo();

            if (elideTableConfig.hasTable(joinModelName)) {
                Table joinTable = elideTableConfig.getTable(joinModelName);
                joinTable.getArguments().forEach(arg -> {
                    String argName = arg.getName();
                    Type argType = arg.getType();
                    if (!(table.hasArgument(argName) && argType == table.getArgument(argName).getType())) {
                        throw new IllegalStateException(String.format(errorMsgPrefix
                                        + "Argument '%s' with type '%s' is not defined but is required by"
                                        + " join table: %s.",
                                        argName, argType, joinModelName));
                    }
                });
            }

            // Must be a static model if not dynamic model
            // Argument Information is not available for static model, so cannot be validation here.
        });
    }

    public static void verifyDefaultValue(Argument argument, String errorMsgPrefix) {

        Set<String> values = argument.getValues();

        if (CollectionUtils.isEmpty(values)) {
            return; // Nothing to validate
        }

        Object defaultValue = argument.getDefaultValue();
        if (defaultValue != null && !values.contains(defaultValue.toString())) {
            throw new IllegalStateException(String.format(errorMsgPrefix
                            + "Default Value for argument '%s' must match one of these values: %s. Found '%s' instead.",
                            argument.getName(), values, defaultValue));
        }
    }
}
