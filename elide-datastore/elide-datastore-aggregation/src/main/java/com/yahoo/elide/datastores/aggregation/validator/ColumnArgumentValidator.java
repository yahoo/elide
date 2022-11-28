/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.validator;

import static com.yahoo.elide.datastores.aggregation.validator.TableArgumentValidator.verifyDefaultValue;
import static com.yahoo.elide.datastores.aggregation.validator.TableArgumentValidator.verifyValue;
import static com.yahoo.elide.datastores.aggregation.validator.TableArgumentValidator.verifyValues;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ColumnArgReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.LogicalReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ReferenceExtractor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies all defined arguments for column.
 * Ensures all arguments required for column are defined.
 * For any column:
 * <ul>
 *   <li>All the column arguments used (Either in column definition Or in Join expressions for join referenced directly
 *    in column definition) must be defined in the argument list for this column (with or without default value)</li>
 *   <li>If the column references another logical column (Either in column definition Or in Join expressions for join
 *    referenced directly in column definition), If any argument in the argument list for this logical column doesn't
 *     have default value then that argument must be defined for this column also (with or without default value). The
 *      only exception is if this logical column is referenced using SQL helper by this column, which defines a fixed
 *       value for this argument, then there is no need to redefine the argument for this column.</li>
 * </ul>
 */
public class ColumnArgumentValidator {

    private final MetaDataStore metaDataStore;
    private final SQLTable table;
    private final Column column;
    private final String errorMsgPrefix;
    private final ExpressionParser parser;

    public ColumnArgumentValidator(MetaDataStore metaDataStore, SQLTable table, Column column) {
        this.metaDataStore = metaDataStore;
        this.table = table;
        this.column = column;
        this.errorMsgPrefix = String.format("Failed to verify column arguments for column: %s in table: %s. ",
                        column.getName(), table.getName());
        this.parser = new ExpressionParser(metaDataStore);
    }

    public void validate() {

        column.getArgumentDefinitions().forEach(arg -> {
            verifyValues(arg, errorMsgPrefix);
            verifyDefaultValue(arg, errorMsgPrefix);
        });

        List<Reference> references = parser.parse(table, column.getExpression());

        ReferenceExtractor<LogicalReference> logicalRefExtractor = new ReferenceExtractor<LogicalReference>(
                        LogicalReference.class, metaDataStore, ReferenceExtractor.Mode.SAME_COLUMN);
        ReferenceExtractor<ColumnArgReference> columnArgRefExtractor = new ReferenceExtractor<ColumnArgReference>(
                        ColumnArgReference.class, metaDataStore, ReferenceExtractor.Mode.SAME_COLUMN);

        references.stream()
                        .map(reference -> reference.accept(columnArgRefExtractor))
                        .flatMap(Set::stream)
                        .map(ColumnArgReference::getArgName)
                        .forEach(argName -> {
                            if (!column.hasArgumentDefinition(argName) && !hasTemplateFilterArgument(argName)) {
                                throw new IllegalStateException(String.format(errorMsgPrefix
                                                + "Argument '%s' is not defined but found '{{$$column.args.%s}}'.",
                                                argName, argName));
                            }
                        });

        references.stream()
                        .map(reference -> reference.accept(logicalRefExtractor))
                        .flatMap(Set::stream)
                        .forEach(this::verifyLogicalReference);
    }

    private void verifyLogicalReference(LogicalReference reference) {

        SQLTable sqlTable = (SQLTable) reference.getSource();
        ColumnProjection columnProj = reference.getColumn();

        // This will have dependent column's defined arguments merged with pinned arguments used to invoke this column.
        Map<String, Argument> mergedArguments = columnProj.getArguments();

        Column refColumn = sqlTable.getColumn(Column.class, columnProj.getName());

        verifyPinnedArguments(mergedArguments, refColumn, String.format(errorMsgPrefix
                        + "Type mismatch of Fixed value provided for Dependent Column: '%s' in table: '%s'. ",
                        refColumn.getName(), sqlTable.getName()));

        refColumn.getArgumentDefinitions().forEach(argDef -> {
            String argName = argDef.getName();

            if (column.hasArgumentDefinition(argName)) {
                if (argDef.getType() != column.getArgumentDefinition(argName).getType()) {
                    throw new IllegalStateException(String.format(errorMsgPrefix
                                    + "Argument type mismatch. Dependent Column: '%s' in table: '%s' has same"
                                    + " Argument: '%s' with type '%s'.",
                                    refColumn.getName(), sqlTable.getName(), argName, argDef.getType()));
                }
            } else if (StringUtils.isBlank(argDef.getDefaultValue().toString())
                            && StringUtils.isBlank(mergedArguments.get(argName).getValue().toString())) {
                throw new IllegalStateException(String.format(errorMsgPrefix
                                + "Argument '%s' with type '%s' is not defined but is required for"
                                + " Dependent Column: '%s' in table: '%s'.",
                                argName, argDef.getType(), refColumn.getName(), sqlTable.getName()));
            }
        });
    }

    /**
     * Verifies pinned value for any argument matches with {@link ArgumentDefinition}.
     * @param mergedArguments Dependent {@link Column}'s defined arguments merged with pinned arguments used to
     *         invoke this column.
     * @param refColumn Original dependent {@link Column}.
     * @param errorMsgPrefix Custom error message.
     */
    private void verifyPinnedArguments(Map<String, Argument> mergedArguments, Column refColumn, String errorMsgPrefix) {
        mergedArguments.forEach((argName, arg) -> {
            Object originalValue = refColumn.getArgumentDefinition(argName).getDefaultValue();

            if (! arg.getValue().equals(originalValue)) {
                verifyValue(refColumn.getArgumentDefinition(argName),
                            arg.getValue().toString(),
                            errorMsgPrefix + "Pinned ");
            }
        });
    }

    private boolean hasTemplateFilterArgument(String argName) {
        return column.getRequiredFilter() != null && column.getRequiredFilter().contains("{{" + argName + "}}");
    }
}
