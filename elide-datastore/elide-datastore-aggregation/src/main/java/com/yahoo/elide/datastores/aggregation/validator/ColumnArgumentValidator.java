/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.validator;

import static com.yahoo.elide.datastores.aggregation.validator.TableArgumentValidator.verifyDefaultValue;
import static com.yahoo.elide.datastores.aggregation.validator.TableArgumentValidator.verifyValue;
import static com.yahoo.elide.datastores.aggregation.validator.TableArgumentValidator.verifyValues;

import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ColumnArgReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.JoinReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.LogicalReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
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

        List<Reference> references =
                        verifyColumnArgsExists(table, column.getExpression(), "in this column's definition.");
        verifyColumnArgsWithDirectlyDependentColumnsArgs(references, "in this column's definition.");
        verifyColumnArgsInJoinExpressions(references);
    }

    private List<Reference> verifyColumnArgsExists(SQLTable table, String expression, String errorMsgSuffix) {
        List<Reference> references = parser.parse(table, expression);
        verifyColumnArgsExists(references, errorMsgSuffix);
        return references;
    }

    private void verifyColumnArgsExists(List<Reference> references, String errorMsgSuffix) {
        references.stream()
                        .filter(ref -> ref instanceof ColumnArgReference)
                        .map(ColumnArgReference.class::cast)
                        .map(ColumnArgReference::getArgName)
                        .forEach(argName -> {
                            if (!column.hasArgumentDefinition(argName)) {
                                throw new IllegalStateException(String.format(errorMsgPrefix
                                                + "Argument '%s' is not defined but found '{{$$ccolumn.args.%s}}' "
                                                + errorMsgSuffix, argName, argName));
                            }
                        });
    }

    private void verifyColumnArgsInJoinExpressions(List<Reference> references) {
        references.stream()
                        .filter(ref -> ref instanceof JoinReference)
                        .map(JoinReference.class::cast)
                        .map(JoinReference::getPath)
                        .map(this::extractJoinExpressionsFromJoinPath)
                        .map(Map::entrySet)
                        .flatMap(Set::stream)
                        .forEach(joinExprKV -> {
                            String joinExpression = joinExprKV.getValue();
                            String errorMsgSuffix = String.format("in join expression: '%s'.", joinExpression);
                            List<Reference> joinExprReferences = verifyColumnArgsExists(joinExprKV.getKey(),
                                                                                        joinExpression,
                                                                                        errorMsgSuffix);
                            verifyColumnArgsWithDirectlyDependentColumnsArgs(joinExprReferences, errorMsgSuffix);
                        });
    }

    private Map<SQLTable, String> extractJoinExpressionsFromJoinPath(JoinPath joinPath) {

        List<PathElement> pathElements = joinPath.getPathElements();
        Map<SQLTable, String> joinExpressions = new HashMap<>();
        SQLTable currentTable = this.table;

        for (int i = 0; i < pathElements.size() - 1; i++) {
            PathElement pathElement = pathElements.get(i);
            Type<?> joinClass = pathElement.getFieldType();
            SQLTable joinTable = metaDataStore.getTable(joinClass);
            String joinFieldName = pathElement.getFieldName();

            SQLJoin sqlJoin = currentTable.getJoin(joinFieldName);
            if (sqlJoin != null) {
                joinExpressions.put(currentTable, sqlJoin.getJoinExpression());
            }
            currentTable = joinTable;
        }

        return joinExpressions;
    }

    /**
     * Verifies dependent column's argument requirements are satisfied by this column.
     */
    private void verifyColumnArgsWithDirectlyDependentColumnsArgs(List<Reference> references, String errorMsgSuffix) {
        references.forEach(reference -> {
            if (reference instanceof LogicalReference) {
                verifyLogicalReference((LogicalReference) reference, errorMsgSuffix);
            } else if (reference instanceof JoinReference) {
                JoinReference joinRef = (JoinReference) reference;
                if (joinRef.getReference() instanceof LogicalReference) {
                    verifyLogicalReference((LogicalReference) joinRef.getReference(), errorMsgSuffix);
                }
            }
        });
    }

    private void verifyLogicalReference(LogicalReference reference, String errorMsgSuffix) {

        SQLTable sqlTable = (SQLTable) reference.getSource();
        ColumnProjection columnProj = reference.getColumn();

        // This will have dependent column's defined arguments merged with pinned arguments used to invoke this column.
        Map<String, Argument> mergedArguments = columnProj.getArguments();

        Column refColumn = sqlTable.getColumn(Column.class, columnProj.getName());

        verifyPinnedArguments(mergedArguments, refColumn, errorMsgSuffix);

        refColumn.getArgumentDefinitions().forEach(argDef -> {
            String argName = argDef.getName();

            if (column.hasArgumentDefinition(argName)) {
                if (argDef.getType() != column.getArgumentDefinition(argName).getType()) {
                    throw new IllegalStateException(String.format(errorMsgPrefix
                                    + "Argument type mismatch. Dependent Column: '%s' %s has same Argument: '%s'"
                                    + " with type '%s'.",
                                    refColumn.getName(), StringUtils.chop(errorMsgSuffix), argName, argDef.getType()));
                }
            } else if (StringUtils.isBlank(argDef.getDefaultValue().toString())
                            && StringUtils.isBlank(mergedArguments.get(argName).getValue().toString())) {
                throw new IllegalStateException(String.format(errorMsgPrefix
                                + "Argument '%s' with type '%s' is not defined but is required by"
                                + " Dependent Column: '%s' %s",
                                argName, argDef.getType(), refColumn.getName(), errorMsgSuffix));
            }
        });
    }

    /**
     * Verifies pinned value for any argument matches with {@link ArgumentDefinition}.
     * @param mergedArguments Dependent {@link Column}'s defined arguments merged with pinned arguments used to
     *         invoke this column.
     * @param refColumn Original dependent {@link Column}.
     * @param errorMsgSuffix Custom error message.
     */
    private void verifyPinnedArguments(Map<String, Argument> mergedArguments, Column refColumn, String errorMsgSuffix) {
        mergedArguments.forEach((argName, arg) -> {
            Object originalValue = refColumn.getArgumentDefinition(argName).getDefaultValue();

            if (! arg.getValue().equals(originalValue)) {
                verifyValue(refColumn.getArgumentDefinition(argName),
                            arg.getValue().toString(),
                            errorMsgPrefix + "Type mismatch for Pinned value " + errorMsgSuffix + " Pinned ");
            }
        });
    }
}
