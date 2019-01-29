/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions;

import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.TRUE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.security.permissions.expressions.Expression.Results.FAILURE;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.CheckInstantiator;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.parsers.expression.FilterExpressionNormalizationVisitor;
import com.yahoo.elide.parsers.expression.PermissionExpressionVisitor;
import com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.permissions.expressions.AnyFieldExpression;
import com.yahoo.elide.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.security.permissions.expressions.Expression;
import com.yahoo.elide.security.permissions.expressions.OrExpression;
import com.yahoo.elide.security.permissions.expressions.SpecificFieldExpression;

import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Expression builder to parse annotations and express the result as the Expression AST.
 */
public class PermissionExpressionBuilder implements CheckInstantiator {
    private final EntityDictionary entityDictionary;
    private final ExpressionResultCache cache;

    private static final Expression SUCCESSFUL_EXPRESSION = OrExpression.SUCCESSFUL_EXPRESSION;
    public static final Expression FAIL_EXPRESSION = OrExpression.FAILURE_EXPRESSION;

    /**
     * Constructor.
     *
     * @param cache Cache
     * @param dictionary EntityDictionary
     */
    public PermissionExpressionBuilder(ExpressionResultCache cache, EntityDictionary dictionary) {
        this.cache = cache;
        this.entityDictionary = dictionary;
    }

    /**
     * Build an expression that checks a specific field.
     *
     * @param resource        Resource
     * @param annotationClass Annotation class
     * @param field           Field
     * @param changeSpec      Change spec
     * @param <A>             Type parameter
     * @return Commit and operation expressions
     */
    public <A extends Annotation> Expression buildSpecificFieldExpressions(final PersistentResource resource,
                                                                           final Class<A> annotationClass,
                                                                           final String field,
                                                                           final ChangeSpec changeSpec) {

        Class<?> resourceClass = resource.getResourceClass();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSION;
        }

        final Function<Check, Expression> leafBuilderFn = leafBuilder(resource, changeSpec);

        final Function<Function<Check, Expression>, Expression> buildExpressionFn =
                (checkFn) -> buildSpecificFieldExpression(
                        PermissionCondition.create(annotationClass, resource, field, changeSpec),
                        checkFn
                );

        return buildExpressionFn.apply(leafBuilderFn);
    }

    /**
     * Build an expression that checks any field on a bean.
     *
     * @param resource        Resource
     * @param annotationClass annotation class
     * @param changeSpec      change spec
     * @param <A>             type parameter
     * @return Commit and operation expressions
     */
    public <A extends Annotation> Expression buildAnyFieldExpressions(final PersistentResource resource,
                                                                       final Class<A> annotationClass,
                                                                       final ChangeSpec changeSpec) {


        Class<?> resourceClass = resource.getResourceClass();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSION;
        }

        final Function<Check, Expression> leafBuilderFn = leafBuilder(resource, changeSpec);

        final Function<Function<Check, Expression>, Expression> expressionFunction =
                (checkFn) -> buildAnyFieldExpression(
                        PermissionCondition.create(annotationClass, resource, (String) null, changeSpec),
                        checkFn,
                        (RequestScope) resource.getRequestScope()
                );

        return expressionFunction.apply(leafBuilderFn);
    }

    /**
     * Build an expression that strictly evaluates UserCheck's and ignores other checks for a specific field.
     * <p>
     * NOTE: This method returns _NO_ commit checks.
     *
     * @param resource        Resource
     * @param annotationClass Annotation class
     * @param field           Field to check (if null only check entity-level)
     * @param <A>             type parameter
     * @return User check expression to evaluate
     */
    public <A extends Annotation> Expression buildUserCheckFieldExpressions(final PersistentResource resource,
                                                                             final Class<A> annotationClass,
                                                                             final String field) {
        Class<?> resourceClass = resource.getResourceClass();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSION;
        }

        final Function<Check, Expression> leafBuilderFn = leafBuilder(resource, null);
        return buildSpecificFieldExpression(new PermissionCondition(annotationClass, resource, field), leafBuilderFn);
    }

    /**
     * Build an expression that strictly evaluates UserCheck's and ignores other checks for an entity.
     * <p>
     * NOTE: This method returns _NO_ commit checks.
     *
     * @param resourceClass   Resource class
     * @param annotationClass Annotation class
     * @param requestScope    Request scope
     * @param <A>             type parameter
     * @return User check expression to evaluate
     */
    public <A extends Annotation> Expression buildUserCheckAnyExpression(final Class<?> resourceClass,
                                                                         final Class<A> annotationClass,
                                                                         final RequestScope requestScope) {

        final Function<Check, Expression> leafBuilderFn = (check) ->
                new CheckExpression(check, null, requestScope, null, cache);

        return buildAnyFieldExpression(
                        new PermissionCondition(annotationClass, resourceClass), leafBuilderFn, requestScope);
    }

    /**
     * Builder for specific field expressions.
     *
     * @param condition       The condition which triggered this permission expression check
     * @param checkFn         Operation check function
     * @return Expressions representing specific field
     */
    private Expression buildSpecificFieldExpression(final PermissionCondition condition,
            final Function<Check, Expression> checkFn) {
        Class<?> resourceClass = condition.getEntityClass();
        Class<? extends Annotation> annotationClass = condition.getPermission();
        String field = condition.getField().isPresent() ? condition.getField().get() : null;

        ParseTree classPermissions = entityDictionary.getPermissionsForClass(resourceClass, annotationClass);
        ParseTree fieldPermissions = entityDictionary.getPermissionsForField(resourceClass, field, annotationClass);

        return new SpecificFieldExpression(condition,
                expressionFromParseTree(classPermissions, checkFn),
                expressionFromParseTree(fieldPermissions, checkFn)
        );
    }

    /**
     * Build an expression representing any field on an entity.
     *
     * @param condition       The condition which triggered this permission expression check
     * @param checkFn         check function
     * @param scope           RequestScope
     * @return Expressions
     */
    private Expression buildAnyFieldExpression(final PermissionCondition condition,
            final Function<Check, Expression> checkFn,
            final RequestScope scope) {

        Class<?> resourceClass = condition.getEntityClass();
        Class<? extends Annotation> annotationClass = condition.getPermission();

        ParseTree classPermissions = entityDictionary.getPermissionsForClass(resourceClass, annotationClass);
        Expression entityExpression = expressionFromParseTree(classPermissions, checkFn);

        OrExpression allFieldsExpression = new OrExpression(FAILURE, null);
        List<String> fields = entityDictionary.getAllFields(resourceClass);
        Set<String> sparseFields = scope.getSparseFields().get(entityDictionary.getJsonAliasFor(resourceClass));

        for (String field : fields) {

            if (sparseFields != null && !sparseFields.contains(field)) {
                continue;
            }

            ParseTree fieldPermissions = entityDictionary.getPermissionsForField(resourceClass, field, annotationClass);
            Expression fieldExpression = expressionFromParseTree(fieldPermissions, checkFn);

            allFieldsExpression = new OrExpression(allFieldsExpression, fieldExpression);
        }

        return new AnyFieldExpression(condition, entityExpression, allFieldsExpression);
    }

    /**
     * Build an expression representing any field on an entity.
     *
     * @param forType   Resource class
     * @param requestScope requestScope
     * @return Expressions
     */
    public FilterExpression buildAnyFieldFilterExpression(Class<?> forType, RequestScope requestScope) {

        Class<? extends Annotation> annotationClass = ReadPermission.class;
        ParseTree classPermissions = entityDictionary.getPermissionsForClass(forType, annotationClass);
        FilterExpression entityFilter = filterExpressionFromParseTree(classPermissions, forType, requestScope);

        //case where the permissions does not have ANY filterExpressionCheck
        if (entityFilter == FALSE_USER_CHECK_EXPRESSION
                || entityFilter == NO_EVALUATION_EXPRESSION
                || entityFilter == TRUE_USER_CHECK_EXPRESSION) {
            entityFilter = null;
        }

        Set<String> sparseFields = requestScope.getSparseFields().get(entityDictionary.getJsonAliasFor(forType));
        FilterExpression allFieldsFilterExpression = entityFilter;
        List<String> fields = entityDictionary.getAllFields(forType).stream()
                .filter(field -> sparseFields == null || sparseFields.contains(field))
                .collect(Collectors.toList());

        for (String field : fields) {
            ParseTree fieldPermissions = entityDictionary.getPermissionsForField(forType, field, annotationClass);
            FilterExpression fieldExpression = filterExpressionFromParseTree(fieldPermissions, forType, requestScope);

            if (fieldExpression == null && entityFilter == null) {
                // When the class FilterExpression and the field FilterExpression are null because at least
                // this field will be visible across all instances
                return null;
            }

            if (fieldExpression == null || fieldExpression == FALSE_USER_CHECK_EXPRESSION) {
                // When the expression is null no permissions have been defined for this field
                // When the expression is FALSE_USER_CHECK_EXPRESSION this field is not accessible to the user
                // In either case this field is not useful for filtering when loading records
                continue;
            }

            if (fieldExpression == NO_EVALUATION_EXPRESSION || fieldExpression == TRUE_USER_CHECK_EXPRESSION) {
                // When the expression is NO_EVALUATION_EXPRESSION we must evaluate check in memory across all instances
                // When the expression is TRUE_USER_CHECK_EXPRESSION all records can be loaded
                return null;
            }

            if (allFieldsFilterExpression != null) {
                allFieldsFilterExpression = new OrFilterExpression(allFieldsFilterExpression, fieldExpression);
            } else {
                allFieldsFilterExpression = fieldExpression;
            }
        }

        return allFieldsFilterExpression;
    }

    private Expression expressionFromParseTree(ParseTree permissions, Function<Check, Expression> checkFn) {
        if (permissions == null) {
            return null;
        }

        return new PermissionExpressionVisitor(entityDictionary, checkFn).visit(permissions);
    }

    private FilterExpression filterExpressionFromParseTree(ParseTree permissions, Class type, RequestScope scope) {
        if (permissions == null) {
            return null;
        }

        FilterExpression expression = new PermissionToFilterExpressionVisitor(entityDictionary, scope, type)
                .visit(permissions);
        return expression.accept(new FilterExpressionNormalizationVisitor());
    }

    private Function<Check, Expression> leafBuilder(PersistentResource resource, ChangeSpec changeSpec) {
        final Function<Check, Expression> leafBuilderFn = (check) -> new CheckExpression(
                check,
                resource,
                resource.getRequestScope(),
                changeSpec,
                cache
        );
        return leafBuilderFn;
    }
}
