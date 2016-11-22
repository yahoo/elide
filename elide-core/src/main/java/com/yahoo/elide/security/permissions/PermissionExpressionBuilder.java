/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.CheckInstantiator;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.parsers.expression.PermissionExpressionVisitor;
import com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.permissions.expressions.AnyFieldExpression;
import com.yahoo.elide.security.permissions.expressions.DeferredCheckExpression;
import com.yahoo.elide.security.permissions.expressions.Expression;
import com.yahoo.elide.security.permissions.expressions.ImmediateCheckExpression;
import com.yahoo.elide.security.permissions.expressions.OrExpression;
import com.yahoo.elide.security.permissions.expressions.SharePermissionExpression;
import com.yahoo.elide.security.permissions.expressions.SpecificFieldExpression;
import com.yahoo.elide.security.permissions.expressions.UserCheckOnlyExpression;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;
import static com.yahoo.elide.security.permissions.expressions.Expression.Results.FAILURE;

/**
 * Expression builder to parse annotations and express the result as the Expression AST.
 */
public class PermissionExpressionBuilder implements CheckInstantiator {
    private final EntityDictionary entityDictionary;
    private final ExpressionResultCache cache;

    private static final Expressions SUCCESSFUL_EXPRESSIONS = new Expressions(
            OrExpression.SUCCESSFUL_EXPRESSION,
            OrExpression.SUCCESSFUL_EXPRESSION
    );

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
    public <A extends Annotation> Expressions buildSpecificFieldExpressions(final PersistentResource resource,
                                                                            final Class<A> annotationClass,
                                                                            final String field,
                                                                            final ChangeSpec changeSpec) {

        Class<?> resourceClass = resource.getResourceClass();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSIONS;
        }

        final Function<Check, Expression> deferredCheckFn = getDeferredExpressionFor(resource, changeSpec);
        final Function<Check, Expression> immediateCheckFn = getImmediateExpressionFor(resource, changeSpec);

        final Function<Function<Check, Expression>, Expression> buildExpressionFn =
                (checkFn) -> buildSpecificFieldExpression(
                        PermissionCondition.create(
                                annotationClass, resource, field, changeSpec
                        ),
                        checkFn
                );

        return new Expressions(
                buildExpressionFn.apply(deferredCheckFn),
                buildExpressionFn.apply(immediateCheckFn)
        );

    }

    /**
     * Build an expression that checks share permissions on a bean.
     *
     * @param resource        Resource
     * @return Commit and operation expressions
     */
    public <A extends Annotation> Expressions buildSharePermissionExpressions(final PersistentResource resource) {

        PermissionCondition condition = new PermissionCondition(SharePermission.class, resource);

        Class<?> resourceClass = resource.getResourceClass();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, SharePermission.class)) {
            SharePermissionExpression unshared = new SharePermissionExpression(condition);
            return new Expressions(unshared, unshared);
        }

        final Function<Check, Expression> deferredCheckFn = getDeferredExpressionFor(resource, null);
        final Function<Check, Expression> immediateCheckFn = getImmediateExpressionFor(resource, null);

        final Function<Function<Check, Expression>, Expression> expressionFunction =
                (checkFn) -> {
                    ParseTree classPermissions = entityDictionary.getPermissionsForClass(resourceClass,
                            SharePermission.class);
                    Expression entityExpression = expressionFromParseTree(classPermissions, checkFn);
                    return new SharePermissionExpression(condition, entityExpression);
                };

        return new Expressions(
                expressionFunction.apply(deferredCheckFn),
                expressionFunction.apply(immediateCheckFn)
        );
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
    public <A extends Annotation> Expressions buildAnyFieldExpressions(final PersistentResource resource,
                                                                       final Class<A> annotationClass,
                                                                       final ChangeSpec changeSpec) {


        Class<?> resourceClass = resource.getResourceClass();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSIONS;
        }

        final Function<Check, Expression> deferredCheckFn = getDeferredExpressionFor(resource, changeSpec);
        final Function<Check, Expression> immediateCheckFn = getImmediateExpressionFor(resource, changeSpec);

        final Function<Function<Check, Expression>, Expression> expressionFunction =
                (checkFn) -> buildAnyFieldExpression(
                        PermissionCondition.create(
                                annotationClass,
                                resource,
                                (String) null,
                                changeSpec),
                        checkFn,
                        (RequestScope) resource.getRequestScope()
                );

        return new Expressions(
                expressionFunction.apply(deferredCheckFn),
                expressionFunction.apply(immediateCheckFn)
        );
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
    public <A extends Annotation> Expressions buildUserCheckFieldExpressions(final PersistentResource resource,
                                                                             final Class<A> annotationClass,
                                                                             final String field) {
        Class<?> resourceClass = resource.getResourceClass();
        if (!entityDictionary.entityHasChecksForPermission(resourceClass, annotationClass)) {
            return SUCCESSFUL_EXPRESSIONS;
        }

        final Function<Check, Expression> userCheckFn =
                (check) -> new UserCheckOnlyExpression(
                        check,
                        resource,
                        resource.getRequestScope(),
                        (ChangeSpec) null,
                        cache
                );

        return new Expressions(
                buildSpecificFieldExpression(new PermissionCondition(annotationClass, resource, field), userCheckFn),
                null
        );
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
    public <A extends Annotation> Expressions buildUserCheckAnyExpression(final Class<?> resourceClass,
                                                                          final Class<A> annotationClass,
                                                                          final RequestScope requestScope) {
        final Function<Check, Expression> userCheckFn =
                (check) -> new UserCheckOnlyExpression(
                        check,
                        (PersistentResource) null,
                        requestScope,
                        (ChangeSpec) null,
                        cache
                );

        return new Expressions(
                buildAnyFieldExpression(
                        new PermissionCondition(annotationClass, resourceClass), userCheckFn, requestScope), null);
    }

    private Function<Check, Expression> getImmediateExpressionFor(PersistentResource resource, ChangeSpec changeSpec) {
        return (check) -> new ImmediateCheckExpression(
                check,
                resource,
                resource.getRequestScope(),
                changeSpec,
                cache
        );
    }

    private Function<Check, Expression> getDeferredExpressionFor(PersistentResource resource, ChangeSpec changeSpec) {
        return (check) -> new DeferredCheckExpression(
                check,
                resource,
                resource.getRequestScope(),
                changeSpec,
                cache
        );
    }

    /**
     * Builder for specific field expressions.
     *
     * @param <A>             type parameter
     * @param condition       The condition which triggered this permission expression check
     * @param checkFn         Operation check function
     * @return Expressions representing specific field
     */
    private <A extends Annotation> Expression buildSpecificFieldExpression(final PermissionCondition condition,
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
     * @param <A>             type parameter
     * @param checkFn         check function
     * @return Expressions
     */
    private <A extends Annotation> Expression buildAnyFieldExpression(final PermissionCondition condition,
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
     * @param <A>             type parameter
     * @param resourceClass   Resource class
     * @param requestScope requestScope
     * @return Expressions
     */
    public <A extends Annotation> FilterExpression buildAnyFieldFilterExpression(Class<?> resourceClass,
                                                                                 RequestScope requestScope) {

        Class<? extends Annotation> annotationClass = ReadPermission.class;
        ParseTree classPermissions = entityDictionary.getPermissionsForClass(resourceClass, annotationClass);
        FilterExpression entityFilterExpression =
                filterExpressionFromParseTree(classPermissions, resourceClass, requestScope);
        //case where the permissions does not have ANY filterExpressionCheck
        if (entityFilterExpression == FALSE_USER_CHECK_EXPRESSION
                || entityFilterExpression == NO_EVALUATION_EXPRESSION) {
            entityFilterExpression = null;
        }
        Set<String> sparseFields = requestScope.getSparseFields().get(entityDictionary.getJsonAliasFor(resourceClass));
        FilterExpression allFieldsFilterExpression = entityFilterExpression;
        List<String> fields = entityDictionary.getAllFields(resourceClass);
        for (String field : fields) {
            // ignore sparse fields
            if (sparseFields != null && !sparseFields.contains(field)) {
                continue;
            }
            ParseTree fieldPermissions = entityDictionary.getPermissionsForField(resourceClass, field, annotationClass);
            FilterExpression fieldExpression =
                    filterExpressionFromParseTree(fieldPermissions, resourceClass, requestScope);
            if (fieldExpression == null || fieldExpression == FALSE_USER_CHECK_EXPRESSION) {
                if (entityFilterExpression == null) {
                    //If the class FilterExpression is also null, we should just return null to allow the field with
                    // null FE be able to see all records.
                    return null;
                }
                continue;
            }
            if (fieldExpression == NO_EVALUATION_EXPRESSION) {
                // For in memory check, we should just return null to allow the field with
                // memory expression be able to see all records.
                return null;
            }
            if (allFieldsFilterExpression == null) {
                allFieldsFilterExpression = fieldExpression;
            } else {
                allFieldsFilterExpression = new OrFilterExpression(allFieldsFilterExpression, fieldExpression);
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

    private FilterExpression filterExpressionFromParseTree(ParseTree permissions, Class entityClass,
            RequestScope requestScope) {
        if (permissions == null) {
            return null;
        }
        FilterExpression permissionFilter = new PermissionToFilterExpressionVisitor(entityDictionary,
                requestScope, entityClass).visit(permissions);
        return permissionFilter;
    }

    /**
     * Structure containing operation-time and commit-time expressions.
     */
    @AllArgsConstructor
    public static class Expressions {
        @Getter private final Expression operationExpression;
        @Getter private final Expression commitExpression;
    }
}
