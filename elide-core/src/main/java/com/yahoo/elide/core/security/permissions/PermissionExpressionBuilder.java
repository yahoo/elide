/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.permissions;

import static com.yahoo.elide.core.security.permissions.expressions.Expression.Results.FAILURE;
import static com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;
import static com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor.TRUE_USER_CHECK_EXPRESSION;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.AnyFieldExpression;
import com.yahoo.elide.core.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;
import com.yahoo.elide.core.security.permissions.expressions.SpecificFieldExpression;
import com.yahoo.elide.core.security.visitors.PermissionExpressionNormalizationVisitor;
import com.yahoo.elide.core.security.visitors.PermissionExpressionVisitor;
import com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor;
import com.yahoo.elide.core.type.Type;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Expression builder to parse annotations and express the result as the Expression AST.
 */
public class PermissionExpressionBuilder {
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

        Type<?> resourceClass = resource.getResourceType();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSION;
        }

        final Function<Check, Expression> leafBuilderFn = leafBuilder(resource, changeSpec);

        final Function<Function<Check, Expression>, Expression> buildExpressionFn =
                (checkFn) -> buildSpecificFieldExpression(
                        PermissionCondition.create(annotationClass, resource, field, changeSpec),
                        checkFn,
                        true
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
                                                                       Set<String> requestedFields,
                                                                       final ChangeSpec changeSpec) {


        Type<?> resourceClass = resource.getResourceType();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSION;
        }

        final Function<Check, Expression> leafBuilderFn = leafBuilder(resource, changeSpec);

        final Function<Function<Check, Expression>, Expression> expressionFunction =
                (checkFn) -> buildAnyFieldExpression(
                        PermissionCondition.create(annotationClass, resource, (String) null, changeSpec),
                        checkFn,
                        requestedFields,
                        resource.getRequestScope()
                );

        return expressionFunction.apply(leafBuilderFn);
    }

    /**
     * Build an expression that strictly evaluates UserCheck's and ignores other checks for a specific field.
     * <p>
     * NOTE: This method returns _NO_ commit checks.
     *
     * @param resourceClass   Resource Class
     * @param scope           The request scope.
     * @param annotationClass Annotation class
     * @param field           Field to check (if null only check entity-level)
     * @param <A>             type parameter
     * @return User check expression to evaluate
     */
    public <A extends Annotation> Expression buildUserCheckFieldExpressions(final Type<?> resourceClass,
                                                                             final RequestScope scope,
                                                                             final Class<A> annotationClass,
                                                                             final String field) {
        return buildUserCheckFieldExpressions(resourceClass, scope, annotationClass, field, true);
    }

    /**
     * Build an expression that strictly evaluates UserCheck's and ignores other checks for a specific field.
     * <p>
     * NOTE: This method returns _NO_ commit checks.
     *
     * @param resourceClass   Resource Class
     * @param scope           The request scope.
     * @param annotationClass Annotation class
     * @param field           Field to check (if null only check entity-level)
     * @param includeEntityPermission whether entity permission needs to be evaluated in the absence of field permission
     * @param <A>             type parameter
     * @return User check expression to evaluate
     */
    public <A extends Annotation> Expression buildUserCheckFieldExpressions(final Type<?> resourceClass,
                                                                            final RequestScope scope,
                                                                            final Class<A> annotationClass,
                                                                            final String field,
                                                                            final boolean includeEntityPermission) {
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSION;
        }

        final Function<Check, Expression> leafBuilderFn = (check) ->
                new CheckExpression(check, null, scope, null, cache);

        return buildSpecificFieldExpression(new PermissionCondition(annotationClass, resourceClass, field),
                leafBuilderFn, includeEntityPermission);
    }

    /**
     * Build an expression that strictly evaluates UserCheck's and ignores other checks for an entity.
     * expression = (field1Rule OR field2Rule ... OR fieldNRule)
     * <p>
     * NOTE: This method returns _NO_ commit checks.
     *
     * @param resourceClass   Resource class
     * @param annotationClass Annotation class
     * @param requestScope    Request scope
     * @param <A>             type parameter
     * @return User check expression to evaluate
     */
    public <A extends Annotation> Expression buildUserCheckAnyExpression(final Type<?> resourceClass,
                                                                         final Class<A> annotationClass,
                                                                         Set<String> requestedFields,
                                                                         final RequestScope requestScope) {

        final Function<Check, Expression> leafBuilderFn = (check) ->
                new CheckExpression(check, null, requestScope, null, cache);

        return buildAnyFieldExpression(
                        new PermissionCondition(annotationClass, resourceClass), leafBuilderFn,
                requestedFields, requestScope);
    }

    /**
     * Build an expression that strictly evaluates UserCheck's and ignores other checks for an entity.
     * expression = (field1Rule OR field2Rule ... OR fieldNRule)
     * <p>
     * NOTE: This method returns _NO_ commit checks.
     *
     * @param resourceClass   Resource class
     * @param annotationClass Annotation class
     * @param requestScope    Request scope
     * @param <A>             type parameter
     * @return User check expression to evaluate
     */
    public <A extends Annotation> Expression buildUserCheckAnyFieldOnlyExpression(final Type<?> resourceClass,
                                                                                  final Class<A> annotationClass,
                                                                                  Set<String> requestedFields,
                                                                                  final RequestScope requestScope) {

        final Function<Check, Expression> leafBuilderFn = (check) ->
                new CheckExpression(check, null, requestScope, null, cache);

        return buildAnyFieldOnlyExpression(
                new PermissionCondition(annotationClass, resourceClass), leafBuilderFn, requestedFields);
    }

    /**
     * Build an expression that strictly evaluates UserCheck's and ignores other checks for an entity.
     * expression = (entityRule AND (field1Rule OR field2Rule ... OR fieldNRule))
     * <p>
     * NOTE: This method returns _NO_ commit checks.
     *
     * @param resourceClass   Resource class
     * @param annotationClass Annotation class
     * @param scope    Request scope
     * @param <A>             type parameter
     * @return User check expression to evaluate
     */
    public <A extends Annotation> Expression buildUserCheckEntityAndAnyFieldExpression(final Type<?> resourceClass,
                                                                                       final Class<A> annotationClass,
                                                                                       Set<String> requestedFields,
                                                                                       final RequestScope scope) {

        final Function<Check, Expression> leafBuilderFn = (check) ->
                new CheckExpression(check, null, scope, null, cache);

        ParseTree classPermissions = entityDictionary.getPermissionsForClass(resourceClass, annotationClass);
        Expression entityExpression = normalizedExpressionFromParseTree(classPermissions, leafBuilderFn);

        Expression anyFieldExpression = buildAnyFieldOnlyExpression(
                new PermissionCondition(annotationClass, resourceClass), leafBuilderFn,
                requestedFields);

        if (entityExpression == null) {
            return anyFieldExpression;
        }

        return new AndExpression(entityExpression, anyFieldExpression);
    }

    /**
     * Builder for specific field expressions.
     *
     * @param condition       The condition which triggered this permission expression check
     * @param checkFn         Operation check function
     * @param includeEntityPermission whether entity permission needs to be evaluated in the absence of field permission
     * @return Expressions representing specific field
     */
    private Expression buildSpecificFieldExpression(final PermissionCondition condition,
            final Function<Check, Expression> checkFn,
            boolean includeEntityPermission) {
        Type<?> resourceClass = condition.getEntityClass();
        Class<? extends Annotation> annotationClass = condition.getPermission();
        String field = condition.getField().orElse(null);

        ParseTree fieldPermissions = entityDictionary.getPermissionsForField(resourceClass, field, annotationClass);

        if (includeEntityPermission) {
            ParseTree classPermissions = entityDictionary.getPermissionsForClass(resourceClass, annotationClass);
            return new SpecificFieldExpression(condition,
                    normalizedExpressionFromParseTree(classPermissions, checkFn),
                    normalizedExpressionFromParseTree(fieldPermissions, checkFn)
            );
        }
        return new SpecificFieldExpression(condition,
                null,
                normalizedExpressionFromParseTree(fieldPermissions, checkFn)
        );
    }

    /**
     * Build an expression representing any field on an entity.
     *
     * @param condition       The condition which triggered this permission expression check
     * @param checkFn         check function
     * @param scope           RequestScope
     * @param requestedFields The list of requested fields
     * @return Expressions
     */
    private Expression buildAnyFieldExpression(final PermissionCondition condition,
            final Function<Check, Expression> checkFn,
            final Set<String> requestedFields,
            final RequestScope scope) {

        Type<?> resourceClass = condition.getEntityClass();
        Class<? extends Annotation> annotationClass = condition.getPermission();

        ParseTree classPermissions = entityDictionary.getPermissionsForClass(resourceClass, annotationClass);
        Expression entityExpression = normalizedExpressionFromParseTree(classPermissions, checkFn);

        OrExpression allFieldsExpression = new OrExpression(FAILURE, null);
        List<String> fields = entityDictionary.getAllExposedFields(resourceClass);

        boolean entityExpressionUsed = false;
        boolean fieldExpressionUsed = false;

        for (String field : fields) {
            if (requestedFields != null && !requestedFields.contains(field)) {
                continue;
            }

            ParseTree fieldPermissions = entityDictionary.getPermissionsForField(resourceClass, field, annotationClass);
            Expression fieldExpression = normalizedExpressionFromParseTree(fieldPermissions, checkFn);

            if (fieldExpression == null) {

                if (entityExpressionUsed) {
                    continue;
                }

                if (entityExpression == null) {
                    //One field had no permissions set - so we allow the action.
                    return SUCCESSFUL_EXPRESSION;
                }

                fieldExpression = entityExpression;
                entityExpressionUsed = true;
            } else {
                fieldExpressionUsed = true;
            }

            allFieldsExpression = new OrExpression(allFieldsExpression, fieldExpression);
        }

        if (! fieldExpressionUsed) {
            //If there are no permissions, allow access...
            if (entityExpression == null) {
                return SUCCESSFUL_EXPRESSION;
            }
            return new AnyFieldExpression(condition, entityExpression);
        }

        return new AnyFieldExpression(condition, allFieldsExpression);
    }

    /**
     * Builds disjunction of permission expression of all requested fields.
     * If the field permission is null, then return default SUCCESSFUL_EXPRESSION.
     * expression = (field1Rule OR field2Rule ... OR fieldNRule)
     * @param condition The condition which triggered this permission expression check
     * @param checkFn check function
     * @param requestedFields The list of requested fields
     * @return Expression
     */
    private Expression buildAnyFieldOnlyExpression(final PermissionCondition condition,
                                                   final Function<Check, Expression> checkFn,
                                                   final Set<String> requestedFields) {
        Type<?> resourceClass = condition.getEntityClass();
        Class<? extends Annotation> annotationClass = condition.getPermission();

        OrExpression allFieldsExpression = new OrExpression(FAILURE, null);
        List<String> fields = entityDictionary.getAllExposedFields(resourceClass);

        boolean fieldExpressionUsed = false;

        for (String field : fields) {
            if (requestedFields != null && !requestedFields.contains(field)) {
                continue;
            }

            ParseTree fieldPermissions = entityDictionary.getPermissionsForField(resourceClass, field, annotationClass);
            Expression fieldExpression = normalizedExpressionFromParseTree(fieldPermissions, checkFn);

            if (fieldExpression == null) {
                return SUCCESSFUL_EXPRESSION;
            }
            fieldExpressionUsed = true;

            allFieldsExpression = new OrExpression(allFieldsExpression, fieldExpression);
        }

        if (!fieldExpressionUsed) {
            return SUCCESSFUL_EXPRESSION;
        }

        return new AnyFieldExpression(condition, allFieldsExpression);
    }


    /**
     * Build an expression representing any field on an entity.
     *
     * @param forType   Resource class
     * @param requestScope requestScope
     * @return Expressions
     */
    public FilterExpression buildAnyFieldFilterExpression(
            Type<?> forType,
            RequestScope requestScope,
            Set<String> requestedFields
    ) {

        Class<? extends Annotation> annotationClass = ReadPermission.class;
        ParseTree classPermissions = entityDictionary.getPermissionsForClass(forType, annotationClass);
        FilterExpression entityFilter = filterExpressionFromParseTree(classPermissions, forType, requestScope);

        //case where the permissions does not have ANY filterExpressionCheck
        if (entityFilter == FALSE_USER_CHECK_EXPRESSION
                || entityFilter == NO_EVALUATION_EXPRESSION
                || entityFilter == TRUE_USER_CHECK_EXPRESSION) {
            entityFilter = null;
        }

        FilterExpression allFieldsFilterExpression = entityFilter;
        List<String> fields = entityDictionary.getAllExposedFields(forType).stream()
                .filter(field -> requestedFields == null || requestedFields.contains(field))
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

    /**
     * Build a filter expression for entity permission alone
     * @param forType Resource class
     * @param requestScope Request Scope
     * @return
     */
    public FilterExpression buildEntityFilterExpression(Type<?> forType, RequestScope requestScope) {
        ParseTree classPermissions = entityDictionary.getPermissionsForClass(forType, ReadPermission.class);
        FilterExpression entityFilter = filterExpressionFromParseTree(classPermissions, forType, requestScope);
        //case where the permissions does not have ANY filterExpressionCheck
        if (entityFilter == FALSE_USER_CHECK_EXPRESSION
                || entityFilter == NO_EVALUATION_EXPRESSION
                || entityFilter == TRUE_USER_CHECK_EXPRESSION) {
            return null;
        }
        return entityFilter;
    }

    private Expression normalizedExpressionFromParseTree(ParseTree permissions, Function<Check, Expression> checkFn) {
        if (permissions == null) {
            return null;
        }

        return permissions
                .accept(new PermissionExpressionVisitor(entityDictionary, checkFn))
                .accept(new PermissionExpressionNormalizationVisitor());
    }

    private FilterExpression filterExpressionFromParseTree(ParseTree permissions, Type type, RequestScope scope) {
        if (permissions == null) {
            return null;
        }
        final Function<Check, Expression> checkFn = (check) ->
                new CheckExpression(check, null, scope, null, cache);

        return normalizedExpressionFromParseTree(permissions, checkFn)
                .accept(new PermissionToFilterExpressionVisitor(entityDictionary, scope, type));
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
