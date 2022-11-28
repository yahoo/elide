/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;
import static com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor.TRUE_USER_CHECK_EXPRESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.EntityPermissions;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.security.permissions.ExpressionResultCache;
import com.yahoo.elide.core.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.visitors.PermissionExpressionNormalizationVisitor;
import com.yahoo.elide.core.security.visitors.PermissionExpressionVisitor;
import com.yahoo.elide.core.security.visitors.PermissionToFilterExpressionVisitor;
import com.yahoo.elide.core.type.Type;
import example.Author;
import example.Book;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("StringEquality")
public class PermissionToFilterExpressionVisitorTest {
    private static final Path.PathElement AUTHOR_PATH = new Path.PathElement(Author.class, Book.class, "books");
    private static final Path.PathElement BOOK_PATH = new Path.PathElement(Book.class, String.class, "title");
    private static final PermissionExpressionNormalizationVisitor NORMALIZATION_VISITOR = new PermissionExpressionNormalizationVisitor();

    private static final FilterExpression IN_PREDICATE = createDummyPredicate(Operator.IN);
    private static final FilterExpression NOT_IN_PREDICATE = createDummyPredicate(Operator.NOT);
    private static final FilterExpression LT_PREDICATE = createDummyPredicate(Operator.LT);
    private static final FilterExpression GE_PREDICATE = createDummyPredicate(Operator.GE);

    private static final String AND = "AND";
    private static final String OR = "OR";
    private static final String AND_NOT = "AND NOT";
    private static final String OR_NOT = "OR NOT";
    private static final List<String> OPS = Arrays.asList(AND, OR, AND_NOT, OR_NOT);

    private static final String USER_ALLOW = "user has all access";
    private static final String USER_DENY = "user has no access";
    private static final String AT_OP_ALLOW = "Operation Allow";
    private static final String AT_OP_DENY = "Operation Deny";
    private static final String IN_FILTER = "in";
    private static final String NOT_IN_FILTER = "notin";
    private static final String LT_FILTER = "lt";
    private static final String GE_FILTER = "ge";
    private static final List<String> PERMISSIONS = Arrays.asList(AT_OP_ALLOW, AT_OP_DENY, USER_ALLOW, USER_DENY,
            IN_FILTER, NOT_IN_FILTER, LT_FILTER, GE_FILTER);

    private EntityDictionary dictionary;
    private RequestScope requestScope;
    private ElideSettings elideSettings;
    private ExpressionResultCache cache;

    @BeforeEach
    public void setupEntityDictionary() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put(AT_OP_ALLOW, Permissions.Succeeds.class);
        checks.put(AT_OP_DENY, Permissions.Fails.class);

        checks.put(USER_ALLOW, Role.ALL.class);
        checks.put(USER_DENY, Role.NONE.class);

        checks.put(IN_FILTER, Permissions.InFilterExpression.class);
        checks.put(NOT_IN_FILTER, Permissions.NotInFilterExpression.class);
        checks.put(LT_FILTER, Permissions.LessThanFilterExpression.class);
        checks.put(GE_FILTER, Permissions.GreaterThanOrEqualFilterExpression.class);

        dictionary = TestDictionary.getTestDictionary(checks);
        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build();

        requestScope = newRequestScope();
        cache = new ExpressionResultCache();
    }

    ///
    /// Test filter extraction
    ///
    public static Stream<Arguments> identityProvider() {
        return PERMISSIONS.stream().map(expression -> Arguments.of(expression, filterFor(expression)));
    }

    @ParameterizedTest
    @MethodSource("identityProvider")
    public void testSingleFilterExpression(String permission, FilterExpression expected) {
        FilterExpression computed = filterExpressionForPermissions(permission);
        assertEquals(expected, computed,  String.format("%s != %s", permission, expected.toString()));
    }

    public static Stream<Arguments> notExpressionProvider() {
        return PERMISSIONS.stream().map(expression -> Arguments.of(expression, negate(filterFor(expression))));
    }

    @ParameterizedTest
    @MethodSource("notExpressionProvider")
    public void testNotFilterExpression(String permission, FilterExpression expected) {
        String expression = String.format("NOT %s", permission);
        FilterExpression computed = filterExpressionForPermissions(expression);
        assertEquals(expected, computed);
    }

    public static Stream<Arguments> simpleAndProvider() {
        return OPS.stream().map(op ->
                PERMISSIONS.stream().map(left ->
                        PERMISSIONS.stream().map(right ->
                                Arguments.of(left, op, right, buildFilter(left, op, right))
                        )
                ).reduce(Stream.empty(), Stream::concat)
        ).reduce(Stream.empty(), Stream::concat);
    }

    @ParameterizedTest
    @MethodSource("simpleAndProvider")
    public void testSimpleExpression(String left, String op, String right, FilterExpression expected) {
        String permission = String.format("%s %s %s", left, op, right);
        FilterExpression computed = filterExpressionForPermissions(permission);
        assertEquals(expected, computed,  String.format("%s != %s", permission, expected));

        boolean specialCase = isSpecialCase(computed);
        boolean isFilterable = containsOnlyFilterableExpressions(computed);
        assertTrue(specialCase || isFilterable, String.format("compound expression contains unfilterable clause '%s'", computed));
    }

    public static Stream<Arguments> nestedExpressionProvider() {
        return OPS.stream().map(outerOp ->
                OPS.stream().map(innerOp ->
                        PERMISSIONS.stream().map(left ->
                                PERMISSIONS.stream().map(innerLeft ->
                                        PERMISSIONS.stream().map(innerRight -> {
                                            String innerPerm =
                                                    String.format("%s %s %s", innerLeft, innerOp, innerRight);

                                            return Arguments.of(
                                                    left,
                                                    outerOp,
                                                    innerPerm,
                                                    buildFilter(left, outerOp, innerLeft, innerOp, innerRight));
                                        })
                                ).reduce(Stream.empty(), Stream::concat)
                        ).reduce(Stream.empty(), Stream::concat)
                ).reduce(Stream.empty(), Stream::concat)
        ).reduce(Stream.empty(), Stream::concat);
    }

    @ParameterizedTest
    @MethodSource("nestedExpressionProvider")
    public void testNestedExpressions(String left, String join, String compound, FilterExpression expected) {
        String permission = String.format("%s %s (%s)", left, join, compound);
        FilterExpression computed = filterExpressionForPermissions(permission);
        assertEquals(expected, computed,  String.format("%s != %s", permission, computed));

        boolean specialCase = isSpecialCase(computed);
        boolean isFilterable = containsOnlyFilterableExpressions(computed);
        assertTrue(specialCase || isFilterable, String.format("compound expression contains unfilterable clause '%s'", computed));
    }

    @Test
    public void testTestMethods() {
        List<FilterExpression> unfilterable = Arrays.asList(
                FALSE_USER_CHECK_EXPRESSION, TRUE_USER_CHECK_EXPRESSION, NO_EVALUATION_EXPRESSION);

        for (FilterExpression e : unfilterable) {
            assertTrue(isSpecialCase(e), String.format("isSpecialCase(%s)", e));
        }


        assertTrue(containsOnlyFilterableExpressions(
                new AndFilterExpression(IN_PREDICATE, NOT_IN_PREDICATE)
        ));
        assertTrue(containsOnlyFilterableExpressions(
                new OrFilterExpression(IN_PREDICATE, NOT_IN_PREDICATE)
        ));

        assertFalse(containsOnlyFilterableExpressions(
                new AndFilterExpression(IN_PREDICATE, FALSE_USER_CHECK_EXPRESSION)
        ));
        assertFalse(containsOnlyFilterableExpressions(
                new OrFilterExpression(IN_PREDICATE, FALSE_USER_CHECK_EXPRESSION)
        ));

        assertFalse(containsOnlyFilterableExpressions(
                new AndFilterExpression(IN_PREDICATE, new AndFilterExpression(IN_PREDICATE, TRUE_USER_CHECK_EXPRESSION))
        ));
        assertFalse(containsOnlyFilterableExpressions(
                new OrFilterExpression(IN_PREDICATE, new OrFilterExpression(IN_PREDICATE, TRUE_USER_CHECK_EXPRESSION))
        ));

        FilterExpression negated, expected;

        negated = negate(new OrFilterExpression(NOT_IN_PREDICATE, NOT_IN_PREDICATE));
        expected = new AndFilterExpression(IN_PREDICATE, IN_PREDICATE);
        assertEquals(expected, negated);
    }

    //
    // Helpers
    //
    public RequestScope newRequestScope() {
        User john = new TestUser("John");
        return requestScope = new RequestScope(null, null, NO_VERSION, null, null, john, null, null, UUID.randomUUID(), elideSettings);
    }

    private FilterExpression filterExpressionForPermissions(String permission) {
        Function<Check, Expression> checkFn = (check) ->
                new CheckExpression(check, null, requestScope, null, cache);
        ParseTree expression = EntityPermissions.parseExpression(permission);
        PermissionToFilterExpressionVisitor fev = new PermissionToFilterExpressionVisitor(dictionary, requestScope, null);
        return expression
                .accept(new PermissionExpressionVisitor(dictionary, checkFn))
                .accept(NORMALIZATION_VISITOR)
                .accept(fev);
    }

    private static boolean isSpecialCase(FilterExpression expr) {
        return expr == FALSE_USER_CHECK_EXPRESSION
                || expr == TRUE_USER_CHECK_EXPRESSION
                || expr == NO_EVALUATION_EXPRESSION;
    }

    private static boolean containsOnlyFilterableExpressions(FilterExpression expr) {
        if (isSpecialCase(expr)) {
            return false;
        }

        FilterExpression left, right;
        if (expr instanceof OrFilterExpression) {
            OrFilterExpression or = (OrFilterExpression) expr;
            left = or.getLeft();
            right = or.getRight();
            return containsOnlyFilterableExpressions(left) && containsOnlyFilterableExpressions(right);
        }
        if (expr instanceof AndFilterExpression) {
            AndFilterExpression and = (AndFilterExpression) expr;
            left = and.getLeft();
            right = and.getRight();
            return containsOnlyFilterableExpressions(left) && containsOnlyFilterableExpressions(right);
        }

        return true;

    }

    private static FilterPredicate createDummyPredicate(Operator operator) {
        List<Path.PathElement> pathList = Arrays.asList(AUTHOR_PATH, BOOK_PATH);
        return new FilterPredicate(new Path(pathList), operator, Collections.singletonList("Harry Potter"));
    }

    private static boolean isFilterPermission(FilterExpression permission) {
        return permission != TRUE_USER_CHECK_EXPRESSION
                && permission != FALSE_USER_CHECK_EXPRESSION
                && permission != NO_EVALUATION_EXPRESSION;
    }

    private static FilterExpression filterFor(String permission) {
        switch (permission) {
            case USER_ALLOW:    return TRUE_USER_CHECK_EXPRESSION;
            case USER_DENY:     return FALSE_USER_CHECK_EXPRESSION;
            case AT_OP_ALLOW:   return NO_EVALUATION_EXPRESSION;
            case AT_OP_DENY:    return NO_EVALUATION_EXPRESSION;
            case IN_FILTER:     return IN_PREDICATE;
            case NOT_IN_FILTER: return NOT_IN_PREDICATE;
            case LT_FILTER:     return LT_PREDICATE;
            case GE_FILTER:     return GE_PREDICATE;
        }
        return null;
    }

    private static FilterExpression negate(FilterExpression expression) {
        if (expression == TRUE_USER_CHECK_EXPRESSION) {
            return FALSE_USER_CHECK_EXPRESSION;
        }
        if (expression == FALSE_USER_CHECK_EXPRESSION) {
            return TRUE_USER_CHECK_EXPRESSION;
        }
        if (expression == NO_EVALUATION_EXPRESSION) {
            return NO_EVALUATION_EXPRESSION;
        }
        if (expression instanceof FilterPredicate) {
            FilterPredicate filter = (FilterPredicate) expression;
            return filter.negate();
        }
        if (expression instanceof AndFilterExpression) {
            AndFilterExpression and = (AndFilterExpression) expression;
            return new OrFilterExpression(negate(and.getLeft()), negate(and.getRight()));
        }
        if (expression instanceof OrFilterExpression) {
            OrFilterExpression or = (OrFilterExpression) expression;
            return new AndFilterExpression(negate(or.getLeft()), negate(or.getRight()));
        }
        return expression;
    }

    private static FilterExpression buildFilter(String left, String op, String right) {
        FilterExpression leftExpr = filterFor(left);
        FilterExpression rightExpr = filterFor(right);
        switch (op) {
            case AND:
                return filterForAndOf(leftExpr, rightExpr);
            case OR:
                return filterForOrOf(leftExpr, rightExpr);
            case AND_NOT:
                return filterForAndOf(leftExpr, negate(filterFor(right)));
            case OR_NOT:
                return filterForOrOf(leftExpr, negate(filterFor(right)));
        }
        return null;
    }

    private static FilterExpression buildFilter(String left, String op, String innerLeft, String innerOp, String innerRight) {
        FilterExpression leftExpr = filterFor(left);
        switch (op) {
            case AND:
                return filterForAndOf(leftExpr, buildFilter(innerLeft, innerOp, innerRight));
            case OR:
                return filterForOrOf(leftExpr, buildFilter(innerLeft, innerOp, innerRight));
            case AND_NOT:
                return filterForAndOf(leftExpr, buildNotFilter(innerLeft, innerOp, innerRight));
            case OR_NOT:
                return filterForOrOf(leftExpr, buildNotFilter(innerLeft, innerOp, innerRight));
        }
        return null;
    }

    private static FilterExpression buildNotFilter(String left, String op, String right) {
        FilterExpression leftExpr = negate(filterFor(left));
        switch (op) {
            case AND:
                return filterForOrOf(leftExpr, negate(filterFor(right)));
            case OR:
                return filterForAndOf(leftExpr, negate(filterFor(right)));
            case AND_NOT:
                return filterForOrOf(leftExpr, filterFor(right));
            case OR_NOT:
                return filterForAndOf(leftExpr, filterFor(right));
        }
        return null;
    }

    private static FilterExpression filterForAndOf(FilterExpression left, FilterExpression right) {
        if (left == FALSE_USER_CHECK_EXPRESSION || right == FALSE_USER_CHECK_EXPRESSION) {
            return FALSE_USER_CHECK_EXPRESSION;
        }

        if (!isFilterPermission(left)) {
            return right;
        }

        if (!isFilterPermission(right)) {
            return left;
        }

        return new AndFilterExpression(left, right);
    }

    private static FilterExpression filterForOrOf(FilterExpression left, FilterExpression right) {
        if (left == TRUE_USER_CHECK_EXPRESSION || left == NO_EVALUATION_EXPRESSION) {
            // left will not filter, return it
            return left;
        }

        if (right == TRUE_USER_CHECK_EXPRESSION || right == NO_EVALUATION_EXPRESSION) {
            // right will not filter, return it
            return right;
        }

        if (left == FALSE_USER_CHECK_EXPRESSION && right == FALSE_USER_CHECK_EXPRESSION) {
            // both false
            return FALSE_USER_CHECK_EXPRESSION;
        }

        if (left == FALSE_USER_CHECK_EXPRESSION) {
            return right;
        }

        if (right == FALSE_USER_CHECK_EXPRESSION) {
            return left;
        }

        return new OrFilterExpression(left, right);
    }


    public static class Permissions {
        public static class Succeeds extends OperationCheck<Object> {
            @Override
            public boolean ok(Object object, com.yahoo.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return true;
            }
        }

        public static class Fails extends OperationCheck<Object> {
            @Override
            public boolean ok(Object object, com.yahoo.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return false;
            }
        }

        public static class InFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Type entityClass, com.yahoo.elide.core.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.IN);
            }
        }

        public static class NotInFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Type entityClass,
                                                       com.yahoo.elide.core.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.NOT);
            }
        }

        public static class LessThanFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Type entityClass, com.yahoo.elide.core.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.LT);
            }
        }

        public static class GreaterThanOrEqualFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Type entityClass, com.yahoo.elide.core.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.GE);
            }
        }
    }
}
