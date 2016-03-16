/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions;

import com.yahoo.elide.audit.InvalidSyntaxException;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.parsers.expressions.ExpressionVisitor;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.ExtractedChecks;
import com.yahoo.elide.security.permissions.expressions.AndExpression;
import com.yahoo.elide.security.permissions.expressions.AnyFieldExpression;
import com.yahoo.elide.security.permissions.expressions.DeferredCheckExpression;
import com.yahoo.elide.security.permissions.expressions.Expression;
import com.yahoo.elide.security.permissions.expressions.ImmediateCheckExpression;
import com.yahoo.elide.security.permissions.expressions.OrExpression;
import com.yahoo.elide.security.permissions.expressions.SpecificFieldExpression;
import com.yahoo.elide.security.permissions.expressions.UserCheckOnlyExpression;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.yahoo.elide.security.checks.ExtractedChecks.CheckMode.ALL;

/**
 * Expression builder to parse annotations and express the result as the Expression AST.
 */
@Slf4j
public class ExpressionBuilder {
    private final EntityDictionary entityDictionary;
    private final ExpressionResultCache cache;

    private static final BiFunction<Expression, Expression, Expression> ALL_JOINER = AndExpression::new;
    private static final BiFunction<Expression, Expression, Expression> ANY_JOINER = OrExpression::new;

    /**
     * Constructor.
     *
     * @param cache Cache
     */
    public ExpressionBuilder(ExpressionResultCache cache, EntityDictionary dictionary) {
        this.cache = cache;
        this.entityDictionary = dictionary;
    }

    /**
     * Build an expression that checks a specific field.
     *
     * @param resource        Resource
     * @param annotationClass Annotation calss
     * @param field           Field
     * @param changeSpec      Change spec
     * @param <A>             Type parameter
     * @return Commit and operation expressions
     */
    public <A extends Annotation> Expressions buildSpecificFieldExpressions(final PersistentResource resource,
                                                                            final Class<A> annotationClass,
                                                                            final String field,
                                                                            final ChangeSpec changeSpec) {
        ParseTree fieldTree = entityDictionary.getEntityFieldParseTree(resource.getResourceClass(),
                annotationClass,
                field);

        if (fieldTree != null) {
            ExpressionVisitor ev = new ExpressionVisitor(resource, resource.getRequestScope(), changeSpec, cache);
            Expression operationExpression = ev.visit(fieldTree);
            Expression commitExpression = ev.visit(fieldTree);
            return new Expressions(operationExpression, commitExpression);

        } else {
            final Function<Check, Expression> deferredCheckFn =
                    (check) -> new DeferredCheckExpression(
                            check,
                            resource,
                            resource.getRequestScope(),
                            changeSpec,
                            cache
                    );

            final Function<Check, Expression> immediateCheckFn =
                    (check) -> new ImmediateCheckExpression(
                            check,
                            resource,
                            resource.getRequestScope(),
                            changeSpec,
                            cache
                    );

            final Function<Function<Check, Expression>, Expression> expressionFunction =
                    (checkFn) -> buildSpecificFieldExpression(
                            resource.getResourceClass(),
                            entityDictionary,
                            annotationClass,
                            field,
                            checkFn
                    );

            return new Expressions(
                    expressionFunction.apply(deferredCheckFn),
                    expressionFunction.apply(immediateCheckFn)
            );
        }
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

        // If the Class has an annotation with an Expression to be parsed,
        //  i.e. (@ReadPermission(expression = "(A OR B)")
        if (entityDictionary.getEntityParseTree(resource.getResourceClass(), annotationClass) != null) {
            ExpressionVisitor ev = new ExpressionVisitor(resource, resource.getRequestScope(), changeSpec, cache);

            Expression operationExpression = ev.visit(entityDictionary.getEntityParseTree(
                    resource.getResourceClass(),
                    annotationClass)
            );
            Expression commitExpression = ev.visit(entityDictionary.getEntityParseTree(
                    resource.getResourceClass(),
                    annotationClass)
            );

            return new Expressions(operationExpression, commitExpression);
        } else {
            final Function<Check, Expression> deferredCheckFn =
                    (check) -> new DeferredCheckExpression(
                            check,
                            resource,
                            resource.getRequestScope(),
                            changeSpec,
                            cache
                    );

            final Function<Check, Expression> immediateCheckFn =
                    (check) -> new ImmediateCheckExpression(
                            check,
                            resource,
                            resource.getRequestScope(),
                            changeSpec,
                            cache
                    );

            final Function<Function<Check, Expression>, Expression> expressionFunction =
                    (checkFn) -> buildAnyFieldExpression(
                            resource.getResourceClass(),
                            this.entityDictionary,
                            annotationClass,
                            checkFn
                    );

            return new Expressions(
                    expressionFunction.apply(deferredCheckFn),
                    expressionFunction.apply(immediateCheckFn)
            );
        }
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
        final Function<Check, Expression> userCheckFn =
                (check) -> new UserCheckOnlyExpression(
                        check,
                        resource,
                        resource.getRequestScope(),
                        (ChangeSpec) null,
                        cache
                );

        return new Expressions(
                buildSpecificFieldExpression(resource.getResourceClass(),
                        entityDictionary,
                        annotationClass,
                        field,
                        userCheckFn
                ),
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
                buildAnyFieldExpression(resourceClass, entityDictionary, annotationClass, userCheckFn),
                null
        );
    }

    /**
     * Builder for specific field expressions.
     *
     * @param resourceClass   Resource class
     * @param dictionary      Dictionary
     * @param annotationClass Annotation class
     * @param field           Field
     * @param checkFn         Operation check function
     * @param <A>             type parameter
     * @return Expressions representing specific field
     */
    private <A extends Annotation> Expression buildSpecificFieldExpression(final Class<?> resourceClass,
                                                                           final EntityDictionary dictionary,
                                                                           final Class<A> annotationClass,
                                                                           final String field,
                                                                           final Function<Check, Expression> checkFn) {
        final ExtractedChecks entityExtracted = new ExtractedChecks(resourceClass, dictionary, annotationClass);
        final ExtractedChecks fieldExtracted = new ExtractedChecks(resourceClass, dictionary, annotationClass, field);

        final BiFunction<Expression, Expression, Expression> entityJoiner = getJoiner(entityExtracted.getCheckMode());
        final BiFunction<Expression, Expression, Expression> fieldJoiner = getJoiner(fieldExtracted.getCheckMode());

        Queue<Class<? extends Check>> entityChecks = arrayToQueue(entityExtracted.getChecks());
        Queue<Class<? extends Check>> fieldChecks = arrayToQueue(fieldExtracted.getChecks());

        return new SpecificFieldExpression(
                buildExpression(entityChecks, checkFn, entityJoiner),
                buildExpression(fieldChecks, checkFn, fieldJoiner)
        );
    }

    /**
     * Build an expression representing any field on an entity.
     *
     * @param resourceClass   Resource class
     * @param dictionary      Dictionary
     * @param annotationClass Annotation class
     * @param checkFn         check function
     * @param <A>             type parameter
     * @return Expressions
     */
    private <A extends Annotation> Expression buildAnyFieldExpression(final Class<?> resourceClass,
                                                                      final EntityDictionary dictionary,
                                                                      final Class<A> annotationClass,
                                                                      final Function<Check, Expression> checkFn) {
        final List<String> fields = dictionary.getAllFields(resourceClass);

        final ExtractedChecks entityExtracted = new ExtractedChecks(resourceClass, dictionary, annotationClass);

        final BiFunction<Expression, Expression, Expression> entityJoiner = getJoiner(entityExtracted.getCheckMode());

        // Initialize first at the entity level
        final Expression entityExp = buildExpression(arrayToQueue(entityExtracted.getChecks()), checkFn, entityJoiner);
        OrExpression allFieldsExpression = null;

        // Combine checks for each field as well
        for (String field : fields) {
            final ExtractedChecks extracted = new ExtractedChecks(resourceClass, dictionary, annotationClass, field);
            final BiFunction<Expression, Expression, Expression> fieldJoiner = getJoiner(extracted.getCheckMode());

            Queue<Class<? extends Check>> fieldChecks = arrayToQueue(extracted.getChecks());

            Expression fieldExpression = buildExpression(fieldChecks, checkFn, fieldJoiner);

            if (fieldExpression != null) {
                allFieldsExpression = new OrExpression(fieldExpression, allFieldsExpression);
            }
        }

        return new AnyFieldExpression(entityExp, allFieldsExpression);
    }

    /**
     * Build a specific expression for a check.
     *
     * @param checks            Checks to build expression for
     * @param expressionBuilder Builder method to convert check to an expression
     * @param expressionJoiner  Method to join checks
     * @return Expression
     */
    private Expression buildExpression(final Queue<Class<? extends Check>> checks,
                                       final Function<Check, Expression> expressionBuilder,
                                       final BiFunction<Expression, Expression, Expression> expressionJoiner) {
        if (checks.size() == 0) {
            return null;
        }

        try {
            Check instance = checks.poll().newInstance();
            Expression expression = expressionBuilder.apply(instance);
            return expressionJoiner.apply(expression, buildExpression(checks, expressionBuilder, expressionJoiner));
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Could not access check. Exception: {}", e);
            throw new InvalidSyntaxException("Could not instantiate specified check.");
        }
    }

    /**
     * Retrieve the proper joiner for a specified check mode.
     *
     * @param mode Mode
     * @return Joiner
     */
    private static BiFunction<Expression, Expression, Expression> getJoiner(ExtractedChecks.CheckMode mode) {
        return (mode == ALL) ? ALL_JOINER : ANY_JOINER;
    }

    /**
     * Convert an array to queue.
     *
     * @param array Array to convert
     * @param <T> Type parameter
     * @return Queue representing array. Empty queue if array is null.
     */
    private static <T> Queue<T> arrayToQueue(final T[] array) {
        return (array == null) ? new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(Arrays.asList(array));
    }

    /**
     * Structure containing built expressions.
     */
    @AllArgsConstructor
    public static class Expressions {
        @Getter private final Expression operationExpression;
        @Getter private final Expression commitExpression;
    }
}
