/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
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

import javax.persistence.Entity;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.TRUE_USER_CHECK_EXPRESSION;

@SuppressWarnings("StringEquality")
public class PermissionToFilterExpressionVisitorTest {
    public static final String USER_ALLOW = "user has all access";
    public static final String USER_DENY = "user has no access";
    public static final String AT_OP_ALLOW = "Operation Allow";
    public static final String AT_OP_DENY = "Operation Deny";
    public static final String IN_FILTER = "in";
    public static final String NOT_IN_FILTER = "notin";
    public static final String LT_FILTER = "lt";
    public static final String GE_FILTER = "ge";

    public static final String AND = "AND";
    public static final String OR = "OR";
    public static final String AND_NOT = "AND NOT";
    public static final String OR_NOT = "OR NOT";

    private static final Path.PathElement AUTHOR_PATH = new Path.PathElement(Author.class, Book.class, "books");
    private static final Path.PathElement BOOK_PATH = new Path.PathElement(Book.class, String.class, "title");
    private static final FilterExpressionNormalizationVisitor NORMALIZATION_VISITOR = new FilterExpressionNormalizationVisitor();
    private static final FilterExpression IN_PREDICATE = createDummyPredicate(Operator.IN);
    private static final FilterExpression NOT_IN_PREDICATE = createDummyPredicate(Operator.NOT);
    private static final List<String> PERMISSIONS = Arrays.asList(
            AT_OP_ALLOW, AT_OP_DENY, USER_ALLOW, USER_DENY, IN_FILTER, NOT_IN_FILTER);
    private static final List<String> OPS = Arrays.asList(AND, OR, AND_NOT, OR_NOT);

    private EntityDictionary dictionary;
    private RequestScope requestScope;
    private ElideSettings elideSettings;

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

        public static class LessThanFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.LT);
            }
        }

        public static class NotInFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.NOT);
            }
        }

        public static class GreaterThanOrEqualFilterExpression extends FilterExpressionCheck {
            @Override
            public FilterPredicate getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
                return createDummyPredicate(Operator.GE);
            }
        }
    }

    private static FilterPredicate createDummyPredicate(Operator operator) {
        List<Path.PathElement> pathList = Arrays.asList(AUTHOR_PATH, BOOK_PATH);
        return new FilterPredicate(new Path(pathList), operator, Collections.singletonList("Harry Potter"));
    }

    private static String notPermission(String permission) {
        switch (permission) {
            case USER_ALLOW:    return USER_DENY;
            case USER_DENY:     return USER_ALLOW;
            case AT_OP_ALLOW:   return AT_OP_DENY;
            case AT_OP_DENY:    return AT_OP_ALLOW;
            case IN_FILTER:     return NOT_IN_FILTER;
            case NOT_IN_FILTER: return IN_FILTER;
        }
        return null;
    }

    private static FilterExpression filterFor(String permission) {
        switch (permission) {
            case USER_ALLOW:    return TRUE_USER_CHECK_EXPRESSION;
            case USER_DENY:     return FALSE_USER_CHECK_EXPRESSION;
            case AT_OP_ALLOW:   return NO_EVALUATION_EXPRESSION;
            case AT_OP_DENY:    return NO_EVALUATION_EXPRESSION;
            case IN_FILTER:     return IN_PREDICATE;
            case NOT_IN_FILTER: return NOT_IN_PREDICATE;
        }
        return null;
    }

    private static boolean buildsFilter(String permission) {
        return permission == IN_FILTER || permission == NOT_IN_FILTER;
    }

    private static FilterExpression filter(String left, String op, String right) {
        switch (op) {
            case AND:
                return filterForAndOf(left, right);
            case OR:
                return filterForOrOf(left, right);
            case AND_NOT:
                return filterForAndOf(left, notPermission(right));
            case OR_NOT:
                return filterForOrOf(left, notPermission(right));
        }
        return null;
    }

    private static FilterExpression filterForAndOf(String left, String right) {
        if (left == USER_DENY || right == USER_DENY) {
            return FALSE_USER_CHECK_EXPRESSION;
        }

        if (buildsFilter(left) && buildsFilter(right)) {
            return new AndFilterExpression(filterFor(left), filterFor(right));
        }

        if (buildsFilter(left)) {
            return filterFor(left);
        }

        if (buildsFilter(right)) {
            return filterFor(right);
        }

        // USER_ALLOW and AT_OP_* result in no filtering
        return NO_EVALUATION_EXPRESSION;
    }
    private static FilterExpression filterForOrOf(String left, String right) {
        if (left == USER_ALLOW || left == AT_OP_ALLOW || left == AT_OP_DENY) {
            // left will not filter, return it
            return left == USER_ALLOW ? TRUE_USER_CHECK_EXPRESSION : NO_EVALUATION_EXPRESSION;
        }

        if (right == USER_ALLOW || right == AT_OP_ALLOW || right == AT_OP_DENY) {
            // right will not filter, return it
            return right == USER_ALLOW ? TRUE_USER_CHECK_EXPRESSION : NO_EVALUATION_EXPRESSION;
        }

        if (left == USER_DENY && right == USER_DENY) {
            // both false
            return FALSE_USER_CHECK_EXPRESSION;
        }

        if (left == USER_DENY) {
            return filterFor(right);
        }

        if (right == USER_DENY) {
            return filterFor(left);
        }

        return new OrFilterExpression(filterFor(left), filterFor(right));
    }

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
        return new Object[][] {
                {AT_OP_ALLOW,   filterFor(AT_OP_ALLOW)},
                {AT_OP_DENY,    filterFor(AT_OP_DENY)},
                {USER_ALLOW,    filterFor(USER_ALLOW)},
                {USER_DENY,     filterFor(USER_DENY)},
                {IN_FILTER,     filterFor(IN_FILTER)},
                {NOT_IN_FILTER, filterFor(NOT_IN_FILTER)}
        };
    }

    @Test(dataProvider = "identity")
    public void testSingleFilterExpression(String permission, FilterExpression expected) {
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, expected.toString()));
    }

    @DataProvider(name = "not_provider")
    public Object[][] notExpressionProvider() {
        return new Object[][] {
                {AT_OP_ALLOW,   filterFor(notPermission(AT_OP_ALLOW))},
                {AT_OP_DENY,    filterFor(notPermission(AT_OP_DENY))},
                {USER_ALLOW,    filterFor(notPermission(USER_ALLOW))},
                {USER_DENY,     filterFor(notPermission(USER_DENY))},
                {IN_FILTER,     filterFor(notPermission(IN_FILTER))},
                {NOT_IN_FILTER, filterFor(notPermission(NOT_IN_FILTER))}
        };
    }

    @Test(dataProvider = "not_provider")
    public void testNotFilterExpression(String permission, FilterExpression expected) {
        String expression = String.format("NOT %s", permission);
        FilterExpression computed = filterExpressionForPermissions(expression);
        Assert.assertEquals(computed, expected);
    }

    @DataProvider(name = "simple_and_provider")
    public Object[][] simpleAndProvider() {
        Object[][] combinations = new Object[PERMISSIONS.size() * PERMISSIONS.size()][3];
        int index = 0;
        for (String left : PERMISSIONS) {
            for (String right : PERMISSIONS) {
                combinations[index][0] = left;
                combinations[index][1] = right;
                combinations[index][2] = filter(left, AND, right);
                index += 1;
            }
        }
        return combinations;
    }

    @Test(dataProvider = "simple_and_provider")
    public void testAndFilterExpression(String left, String right, FilterExpression expected) {
        String permission = String.format("%s AND %s", left, right);
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, expected));
    }

    @DataProvider(name = "simple_or_provider")
    public Object[][] simpleORProvider() {
        Object[][] combinations = new Object[PERMISSIONS.size() * PERMISSIONS.size()][3];
        int index = 0;
        for (String left : PERMISSIONS) {
            for (String right : PERMISSIONS) {
                combinations[index][0] = left;
                combinations[index][1] = right;
                combinations[index][2] = filter(left, OR, right);
                index += 1;
            }
        }
        return combinations;
    }

    @Test(dataProvider = "simple_or_provider")
    public void testOrFilterExpression(String left, String right, FilterExpression expected) {
        String permission = String.format("%s OR %s", left, right);
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, expected));
    }

    @DataProvider(name = "and_not_provider")
    public Object[][] andNotProvider() {
        Object[][] combinations = new Object[PERMISSIONS.size() * PERMISSIONS.size()][3];
        int index = 0;
        for (String left : PERMISSIONS) {
            for (String right : PERMISSIONS) {
                combinations[index][0] = left;
                combinations[index][1] = right;
                combinations[index][2] = filter(left, AND_NOT, right);
                index += 1;
            }
        }
        return combinations;
    }

    @Test(dataProvider = "and_not_provider")
    public void testAndNotFilterExpression(String left, String right, FilterExpression expected) {
        String permission = String.format("%s AND NOT %s", left, right);
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, expected));
    }

    @DataProvider(name = "or_not_provider")
    public Object[][] orNotProvider() {
        Object[][] combinations = new Object[PERMISSIONS.size() * PERMISSIONS.size()][3];
        int index = 0;
        for (String left : PERMISSIONS) {
            for (String right : PERMISSIONS) {
                combinations[index][0] = left;
                combinations[index][1] = right;
                combinations[index][2] = filter(left, OR_NOT, right);
                index += 1;
            }
        }
        return combinations;
    }

    @Test(dataProvider = "or_not_provider")
    public void testOrNotFilterExpression(String left, String right, FilterExpression expected) {
        String permission = String.format("%s OR NOT %s", left, right);
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected, String.format("%s != %s", permission, expected));
    }

    @DataProvider(name = "or_nested_provider")
    public Object[][] orNestedProvider() {
        Object[][] tests = new Object[PERMISSIONS.size() * (OPS.size() * PERMISSIONS.size() * PERMISSIONS.size())][3];
        int index = 0;
        for (String left : PERMISSIONS) {
            for (String op : OPS) {
                for (String innerLeft : PERMISSIONS) {
                    for (String innerRight : PERMISSIONS) {
                        tests[index][0] = left;
                        tests[index][1] = String.format("%s %s %s", innerLeft, op, innerRight);
                        tests[index][2] = filter(left, OR, filter(innerLeft, op, innerRight));
                        index += 1;
                    }
                }

            }
        }

        return tests;
    }

    @Test(dataProvider = "or_nested_provider")
    public void testOrNestedFilterExpression(String left, String compound, FilterExpression expected) {
        String permission = String.format("%s OR (%s)", left, compound);
        FilterExpression computed = filterExpressionForPermissions(permission);
        Assert.assertEquals(computed, expected);
    }

    @Test
    public void testOrNestedAndTrueUserChecks() {
        @Entity
        @Include
        @ReadPermission(expression = IN_FILTER + " OR (" + USER_ALLOW + " AND " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertSame(extracted, TRUE_USER_CHECK_EXPRESSION);
    }

    @Test
    public void testOrNestedAndFilterCheckUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = IN_FILTER + " OR (" + IN_FILTER + " AND " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression dummy = createDummyPredicate(Operator.IN);
        FilterExpression expected = new OrFilterExpression(dummy, dummy);
        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertEquals(extracted, expected);
    }

    @Test
    public void testOrNestedOrFilterCheckUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = IN_FILTER + " OR (" + IN_FILTER + " OR " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertSame(extracted, TRUE_USER_CHECK_EXPRESSION);
    }

    @Test
    public void testOrNestedOrFilterCheckNotUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = IN_FILTER + " OR (" + IN_FILTER + " OR NOT " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertSame(extracted, IN_PREDICATE);
    }

    @Test
    public void testOrNestedAndFilterCheckNotUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = IN_FILTER + " OR (" + IN_FILTER + " AND NOT " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertSame(extracted, IN_PREDICATE);
    }

    @Test
    public void testOperationAndNestedOrFilterChecks() {
        @Entity
        @Include
        @ReadPermission(expression = AT_OP_ALLOW + " AND (" + IN_FILTER + " OR " + IN_FILTER + ")")
        class TargetClass {
        }

        FilterExpression expected = new OrFilterExpression(IN_PREDICATE, IN_PREDICATE);
        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertEquals(extracted, expected);
    }

    @Test
    public void testNestedAndOrWithOnlyUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = USER_ALLOW + " AND (" + USER_ALLOW + " OR " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertSame(extracted, TRUE_USER_CHECK_EXPRESSION);
    }

    @Test
    public void testNestedOrAndWithOperationUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = USER_ALLOW + " OR (" + AT_OP_ALLOW + " AND " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertSame(extracted, TRUE_USER_CHECK_EXPRESSION);
    }

    @Test
    public void testNestedAndOrWithOperationUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = USER_ALLOW + " AND (" + AT_OP_ALLOW + " OR " + USER_ALLOW + ")")
        class TargetClass {
        }

        FilterExpression extracted = extractFilterExpression(TargetClass.class);
        Assert.assertSame(extracted, TRUE_USER_CHECK_EXPRESSION);
    }

    ///
    /// Test filter negation
    ///
    @Test
    public void testNotIn() {
        FilterExpression expression = filterExpressionForPermissions("NOT (" + IN_FILTER + ")");
        FilterExpression negated = filterExpressionForPermissions(NOT_IN_FILTER);
        Assert.assertEquals(expression, negated);
    }

    @Test
    public void testNotLT() {
        FilterExpression expression = filterExpressionForPermissions("NOT (" + LT_FILTER + ")");
        FilterExpression negated = filterExpressionForPermissions(GE_FILTER);
        Assert.assertEquals(expression, negated);
    }

    @Test
    public void testNotWithAnd() {
        //Test Not with AND FilterExpression
        @Entity
        @Include
        @ReadPermission(expression = "NOT (" + IN_FILTER + " AND " + LT_FILTER + ")")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = NOT_IN_FILTER + " OR " + GE_FILTER)
        class TargetClassNegated {
        }
        assertNegatedPermissionEquals(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotWithOr() {
        //Test Not with OR FilterExpression
        @Entity
        @Include
        @ReadPermission(expression = "NOT (" + IN_FILTER + " OR " + LT_FILTER + ")")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = NOT_IN_FILTER + " AND " + GE_FILTER)
        class TargetClassNegated {
        }
        assertNegatedPermissionEquals(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotNested() {
        //Test Not with nested FilterExpression
        @Entity
        @Include
        @ReadPermission(expression = "NOT (" + IN_FILTER + " OR (" + LT_FILTER + " AND (" + NOT_IN_FILTER + ")))")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = NOT_IN_FILTER + " AND (" + GE_FILTER + " OR " + IN_FILTER + ")")
        class TargetClassNegated {
        }
        assertNegatedPermissionEquals(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotWithOperationCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "NOT " + AT_OP_ALLOW)
        class TargetClass {
        }

        FilterExpression fe = extractFilterExpression(TargetClass.class);
        Assert.assertTrue(fe == NO_EVALUATION_EXPRESSION);
    }

    public RequestScope newRequestScope() {
        User john = new User("John");
        return requestScope = new RequestScope(null, null, null, john, null, elideSettings, false);
    }

    private void assertNegatedPermissionEquals(Class target, Class targetNegated) {
        dictionary.bindEntity(target);
        dictionary.bindEntity(targetNegated);
        ParseTree expression1 = dictionary.getPermissionsForClass(target, ReadPermission.class);
        ParseTree expression2 = dictionary.getPermissionsForClass(targetNegated, ReadPermission.class);
        PermissionToFilterExpressionVisitor fev1, fev2;
        fev1 = new PermissionToFilterExpressionVisitor(dictionary, requestScope, target);
        fev2 = new PermissionToFilterExpressionVisitor(dictionary, requestScope, targetNegated);
        FilterExpression result1 = fev1.visit(expression1).accept(NORMALIZATION_VISITOR);
        FilterExpression result2 = fev2.visit(expression2).accept(NORMALIZATION_VISITOR);
        Assert.assertEquals(result1, result2);
    }

    private FilterExpression filterExpressionForPermissions(String permission) {
        ParseTree expression = EntityPermissions.parseExpression(permission);
        PermissionToFilterExpressionVisitor fev = new PermissionToFilterExpressionVisitor(dictionary, requestScope, null);
        return fev.visit(expression).accept(NORMALIZATION_VISITOR);
    }

    private FilterExpression extractFilterExpression(Class targetClass) {
        dictionary.bindEntity(targetClass);
        ParseTree expression = dictionary.getPermissionsForClass(targetClass, ReadPermission.class);
        PermissionToFilterExpressionVisitor fev = new PermissionToFilterExpressionVisitor(dictionary, requestScope, targetClass);
        return fev.visit(expression).accept(NORMALIZATION_VISITOR);
    }
}
