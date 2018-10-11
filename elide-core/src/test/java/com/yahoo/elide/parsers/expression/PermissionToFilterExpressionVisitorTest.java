/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.TRUE_USER_CHECK_EXPRESSION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.EntityPermissions;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.prefab.Role;

import example.Author;
import example.Book;

import org.antlr.v4.runtime.tree.ParseTree;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("StringEquality")
public class PermissionToFilterExpressionVisitorTest {
    private static final Path.PathElement AUTHOR_PATH = new Path.PathElement(Author.class, Book.class, "books");
    private static final Path.PathElement BOOK_PATH = new Path.PathElement(Book.class, String.class, "title");
    private static final FilterExpressionNormalizationVisitor NORMALIZATION_VISITOR = new FilterExpressionNormalizationVisitor();

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

    @BeforeMethod
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

        dictionary = new EntityDictionary(checks);
        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build();

        requestScope = newRequestScope();
    }

    ///
    /// Test filter extraction
    ///
    @DataProvider(name = "identity")
    public Object[][] identityProvider() {
        Object[][] tests = new Object[PERMISSIONS.size()][2];
        int index = 0;
        for (String expression : PERMISSIONS) {
            tests[index][0] = expression;
            tests[index][1] = filterFor(expression);
            index += 1;
        }
        return tests;
    }

    @Test(dataProvider = "identity")
    public void testSingleFilterExpression(String permission, FilterExpression expected) {
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, expected.toString()));
    }

    @DataProvider(name = "not_provider")
    public Object[][] notExpressionProvider() {
        Object[][] tests = new Object[PERMISSIONS.size()][2];
        int index = 0;
        for (String expression : PERMISSIONS) {
            tests[index][0] = expression;
            tests[index][1] = negate(filterFor(expression));
            index += 1;
        }
        return tests;
    }

    @Test(dataProvider = "not_provider")
    public void testNotFilterExpression(String permission, FilterExpression expected) {
        String expression = String.format("NOT %s", permission);
        FilterExpression computed = filterExpressionForPermissions(expression);
        Assert.assertEquals(computed, expected);
    }

    @DataProvider(name = "simple_provider")
    public Object[][] simpleAndProvider() {
        Object[][] tests = new Object[OPS.size() * PERMISSIONS.size() * PERMISSIONS.size()][4];
        int index = 0;
        for (String op : OPS) {
            for (String left : PERMISSIONS) {
                for (String right : PERMISSIONS) {
                    tests[index][0] = left;
                    tests[index][1] = op;
                    tests[index][2] = right;
                    tests[index][3] = buildFilter(left, op, right);
                    index += 1;
                }
            }
        }
        return tests;
    }

    @Test(dataProvider = "simple_provider")
    public void testSimpleExpression(String left, String op, String right, FilterExpression expected) {
        String permission = String.format("%s %s %s", left, op, right);
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, expected));

        boolean specialCase = isSpecialCase(computed);
        boolean isFilterable = containsOnlyFilterableExpressions(computed);
        Assert.assertTrue(specialCase || isFilterable, String.format("compound expression contains unfilterable clause '%s'", computed));
    }

    @DataProvider(name = "nested_provider")
    public Object[][] nestedExpressionProvider() {
        Object[][] tests = new Object[OPS.size() * PERMISSIONS.size() * (OPS.size() * PERMISSIONS.size() * PERMISSIONS.size())][4];
        int index = 0;
        for (String outerOp : OPS) {
            for (String innerOp : OPS) {
                for (String left : PERMISSIONS) {
                    for (String innerLeft : PERMISSIONS) {
                        for (String innerRight : PERMISSIONS) {
                            String innerPerm = String.format("%s %s %s", innerLeft, innerOp, innerRight);
                            FilterExpression innerExpr = buildFilter(innerLeft, innerOp, innerRight);

                            tests[index][0] = left;
                            tests[index][1] = outerOp;
                            tests[index][2] = innerPerm;
                            tests[index][3] = buildFilter(left, outerOp, innerExpr);
                            index += 1;
                        }
                    }

                }
            }
        }
        return tests;
    }

    @Test(dataProvider = "nested_provider")
    public void testNestedExpressions(String left, String join, String compound, FilterExpression expected) {
        String permission = String.format("%s %s (%s)", left, join, compound);
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, computed));

        boolean specialCase = isSpecialCase(computed);
        boolean isFilterable = containsOnlyFilterableExpressions(computed);
        Assert.assertTrue(specialCase || isFilterable, String.format("compound expression contains unfilterable clause '%s'", computed));
    }

    @Test
    public void testTestMethods() {
        List<FilterExpression> unfilterable = Arrays.asList(
                FALSE_USER_CHECK_EXPRESSION, TRUE_USER_CHECK_EXPRESSION, NO_EVALUATION_EXPRESSION);

        for (FilterExpression e : unfilterable) {
            Assert.assertTrue(isSpecialCase(e), String.format("isSpecialCase(%s)", e));
        }


        Assert.assertTrue(containsOnlyFilterableExpressions(
                new AndFilterExpression(IN_PREDICATE, NOT_IN_PREDICATE)
        ));
        Assert.assertTrue(containsOnlyFilterableExpressions(
                new OrFilterExpression(IN_PREDICATE, NOT_IN_PREDICATE)
        ));

        Assert.assertFalse(containsOnlyFilterableExpressions(
                new AndFilterExpression(IN_PREDICATE, FALSE_USER_CHECK_EXPRESSION)
        ));
        Assert.assertFalse(containsOnlyFilterableExpressions(
                new OrFilterExpression(IN_PREDICATE, FALSE_USER_CHECK_EXPRESSION)
        ));

        Assert.assertFalse(containsOnlyFilterableExpressions(
                new AndFilterExpression(IN_PREDICATE, new AndFilterExpression(IN_PREDICATE, TRUE_USER_CHECK_EXPRESSION))
        ));
        Assert.assertFalse(containsOnlyFilterableExpressions(
                new OrFilterExpression(IN_PREDICATE, new OrFilterExpression(IN_PREDICATE, TRUE_USER_CHECK_EXPRESSION))
        ));

        FilterExpression negated, expected;

        negated = negate(new OrFilterExpression(NOT_IN_PREDICATE, NOT_IN_PREDICATE));
        expected = new AndFilterExpression(IN_PREDICATE, IN_PREDICATE);
        Assert.assertEquals(negated, expected);
    }

    //
    // Helpers
    //
    public RequestScope newRequestScope() {
        User john = new User("John");
        return requestScope = new RequestScope(null, null, null, john, null, elideSettings, false);
    }

    private FilterExpression filterExpressionForPermissions(String permission) {
        ParseTree expression = EntityPermissions.parseExpression(permission);
        PermissionToFilterExpressionVisitor fev = new PermissionToFilterExpressionVisitor(dictionary, requestScope, null);
        return fev.visit(expression).accept(NORMALIZATION_VISITOR);
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
        } else if (expr instanceof AndFilterExpression) {
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

    private static FilterExpression buildFilter(String left, String op, FilterExpression rightExpr) {
        FilterExpression leftExpr = filterFor(left);
        switch (op) {
            case AND:
                return filterForAndOf(leftExpr, rightExpr);
            case OR:
                return filterForOrOf(leftExpr, rightExpr);
            case AND_NOT:
                return filterForAndOf(leftExpr, negate(rightExpr));
            case OR_NOT:
                return filterForOrOf(leftExpr, negate(rightExpr));
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
            public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return true;
            }
        }

        public static class Fails extends OperationCheck<Object> {
            @Override
            public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return false;
            }
        }

        public static class InFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.IN);
            }
        }

        public static class NotInFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Class entityClass,
                                                       com.yahoo.elide.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.NOT);
            }
        }

        public static class LessThanFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.LT);
            }
        }

        public static class GreaterThanOrEqualFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.GE);
            }
        }
    }
}
