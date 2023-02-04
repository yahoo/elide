/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.ArgumentType;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.core.security.executors.AggregationStorePermissionExecutor;
import com.yahoo.elide.core.type.AccessibleObject;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.modelconfig.validator.PermissionExpressionVisitor;
import org.antlr.v4.runtime.tree.ParseTree;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
@Builder
@ToString
public class AggregationDataStore implements DataStore {
    @NonNull private final QueryEngine queryEngine;
    private final Cache cache;
    private final Set<Type<?>> dynamicCompiledClasses;
    private final QueryLogger queryLogger;

    public static final Predicate<AccessibleObject> IS_FIELD_HIDDEN = (field -> {
        ColumnMeta meta = field.getAnnotation(ColumnMeta.class);
        Join join = field.getAnnotation(Join.class);

        return (join != null || (meta != null && meta.isHidden()));
    });

    public static final Predicate<Type<?>> IS_TYPE_HIDDEN = (type -> {
        TableMeta meta = type.getAnnotation(TableMeta.class);

        return (meta != null && meta.isHidden());
    });

    private final Function<RequestScope, PermissionExecutor> aggPermissionExecutor =
            AggregationStorePermissionExecutor::new;

    /**
     * These are the classes the Aggregation Store manages.
     */
    private static final List<Class<? extends Annotation>> AGGREGATION_STORE_CLASSES =
            Arrays.asList(FromTable.class, FromSubquery.class);

    /**
     * Populate an {@link EntityDictionary} and use this dictionary to construct a {@link QueryEngine}.
     * @param dictionary the dictionary
     */
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {

        if (dynamicCompiledClasses != null && dynamicCompiledClasses.size() != 0) {
            dynamicCompiledClasses.stream()
                    .filter((type) -> ! IS_TYPE_HIDDEN.test(type))
                    .forEach(dynamicLoadedClass -> {
                dictionary.bindEntity(dynamicLoadedClass, IS_FIELD_HIDDEN);
                validateModelExpressionChecks(dictionary, dynamicLoadedClass);
                dictionary.bindPermissionExecutor(dynamicLoadedClass, aggPermissionExecutor);
            });
        }

        dictionary.getScanner().getAnnotatedClasses(AGGREGATION_STORE_CLASSES).stream()
                .filter((type) -> ! IS_TYPE_HIDDEN.test(ClassType.of(type)))
                .forEach(cls -> {
                    dictionary.bindEntity(cls, IS_FIELD_HIDDEN);
                    validateModelExpressionChecks(dictionary, ClassType.of(cls));
                    dictionary.bindPermissionExecutor(cls, aggPermissionExecutor);
                }
        );

        for (Table table : queryEngine.getMetaDataStore().getMetaData(ClassType.of(Table.class))) {
            /* Add 'grain' argument to each TimeDimensionColumn */
            for (TimeDimension timeDim : table.getAllTimeDimensions()) {
                dictionary.addArgumentToAttribute(
                        dictionary.getEntityClass(table.getName(), table.getVersion()),
                        timeDim.getName(),
                        new ArgumentType("grain", ClassType.STRING_TYPE, timeDim.getDefaultGrain().getGrain()));
            }

            /* Add argument to each Column */
            for (Column col : table.getAllColumns()) {
                for (ArgumentDefinition arg : col.getArgumentDefinitions()) {
                    dictionary.addArgumentToAttribute(
                            dictionary.getEntityClass(table.getName(), table.getVersion()),
                            col.getName(),
                            new ArgumentType(arg.getName(), ValueType.getType(arg.getType()), arg.getDefaultValue()));
                }
            }

            /* Add argument to each Table */
            for (ArgumentDefinition arg : table.getArgumentDefinitions()) {
                dictionary.addArgumentToEntity(
                        dictionary.getEntityClass(table.getName(), table.getVersion()),
                        new ArgumentType(arg.getName(), ValueType.getType(arg.getType()), arg.getDefaultValue()));
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine, cache, queryLogger);
    }

    /**
     * Validates The security Check expression type for both Table and all its fields.
     * Table Security Check Condition - User Checks and Filter Expression Checks
     * Field Security Check Condition - User Checks
     * @param dictionary - Entity Dictionary
     * @param clz - Model Type.
     */
    private void validateModelExpressionChecks(EntityDictionary dictionary, Type<?> clz) {
        PermissionExpressionVisitor visitor = new PermissionExpressionVisitor();
        ParseTree parseTree = dictionary.getPermissionsForClass(clz, ReadPermission.class);
        if (parseTree != null) {
            validateExpression(dictionary,
                    visitor.visit(parseTree),
                    (checkClass) -> UserCheck.class.isAssignableFrom(checkClass)
                            || FilterExpressionCheck.class.isAssignableFrom(checkClass),
                    "Table Can only have User Check and Filter Expression Check."
                    + "Operation Checks Not allowed. given - %s");
        }

        dictionary.getAllExposedFields(clz).stream()
                .map(field -> dictionary.getPermissionsForField(clz, field, ReadPermission.class))
                .filter(Objects::nonNull)
                .forEach(tree ->
                        validateExpression(dictionary,
                                visitor.visit(tree),
                                (checkClass) -> UserCheck.class.isAssignableFrom(checkClass),
                                "Fields Can only have User checks. Given - %s"));
    }

    /**
     * Validates the Expression Check class to check whether it complies with the given predicate.
     * @param dictionary - Entity dictionary
     * @param expressionChecksIdentifiers - Set of identifiers for whose the security check class is validated
     * @param validCheckPredicate - Predicate that takes security check class as argument.
     * @param errorMsgFormat - Error message format for the exception when predicate fails.
     * @throws IllegalStateException
     */
    private void validateExpression(EntityDictionary dictionary,
                                    Set<String> expressionChecksIdentifiers,
                                    Predicate<Class> validCheckPredicate,
                                    String errorMsgFormat) throws IllegalStateException {
        expressionChecksIdentifiers.stream()
                .filter(check -> dictionary.getRoleCheck(check) == null)  // skip all role checks
                .forEach(check -> {
                    Class<? extends Check> checkClass = dictionary.getCheck(check);
                    if (!validCheckPredicate.test(checkClass)) {
                        throw new IllegalStateException(String.format(errorMsgFormat,
                                "(" + check + "-" + checkClass + ")"));
                    }
                });
    }

    /**
     * Determines if a model is managed by the aggregation data store.
     * @param model The model in question.
     * @return True if the model is managed by the aggregation data store.  False otherwise.
     */
    public static final boolean isAggregationStoreModel(Type<?> model) {
        return AGGREGATION_STORE_CLASSES.stream()
                .anyMatch((annotation) -> model.getDeclaredAnnotation(annotation) != null);
    }
}
