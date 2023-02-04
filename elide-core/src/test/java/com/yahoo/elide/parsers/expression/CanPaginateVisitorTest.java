/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.core.security.visitors.CanPaginateVisitor;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CanPaginateVisitorTest {

    private static Map<String, Class<? extends Check>> checkMappings;

    public static final class TestOperationCheck extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object,
                          com.yahoo.elide.core.security.RequestScope requestScope,
                          Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }

    public static final class FalseUserCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            return false;
        }
    }

    public static final class TrueUserCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            return true;
        }
    }

    public static final class TestFilterExpressionCheck extends FilterExpressionCheck<Object> {
        @Override
        public FilterExpression getFilterExpression(Type entityClass, com.yahoo.elide.core.security.RequestScope requestScope) {
            return null;
        }
    }


    @BeforeAll
    public static void init() {
        checkMappings = new HashMap<>();
        checkMappings.put("In Memory Check", TestOperationCheck.class);
        checkMappings.put("False User Check", FalseUserCheck.class);
        checkMappings.put("True User Check", TrueUserCheck.class);
        checkMappings.put("Filter Expression Check", TestFilterExpressionCheck.class);
    }


    @Test
    public void testNoPermissions() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);

        RequestScope scope = mock(RequestScope.class);

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testClassOperationPermissions() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @ReadPermission(expression = "In Memory Check")
        class Book {
            @Id
            private long id;
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertFalse(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testClassUserPermissions() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @ReadPermission(expression = "False User Check")
        class Book {
            @Id
            private long id;
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testFieldFilterPermissions() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "Filter Expression Check")
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testComplexTrueExpression() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression =
                    "(Filter Expression Check AND False User Check) OR (Filter Expression Check OR NOT False User Check)")
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testFalseUserOROperationExpression() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "False User Check OR In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertFalse(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testTrueUserOROperationExpression() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "True User Check OR In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testFalseUserAndOperationExpression() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "False User Check AND In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testTrueUserAndOperationExpression() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "True User Check AND In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertFalse(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testNotOperationExpression() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "NOT In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertFalse(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testMultipleFieldsNoPagination() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "Filter Expression Check")
            private String title;

            @ReadPermission(expression = "In Memory Check")
            private Date publicationDate;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertFalse(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testMultipleFieldsPagination() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @ReadPermission(expression = "In Memory Check")
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "Filter Expression Check")
            private String title;

            @ReadPermission(expression = "Filter Expression Check")
            private Date publicationDate;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, new HashSet<>()));
    }

    @Test
    public void testSparseFields() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @ReadPermission(expression = "In Memory Check")
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "Filter Expression Check")
            private String title;

            @ReadPermission(expression = "Filter Expression Check")
            private Date publicationDate;

            private boolean outOfPrint;
        }

        EntityDictionary dictionary = TestDictionary.getTestDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Set<String> sparseFields = new HashSet<>();

        assertFalse(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, sparseFields));
        sparseFields.add("title");
        sparseFields.add("publicationDate");

        assertTrue(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, sparseFields));

        sparseFields.add("outOfPrint");

        assertFalse(CanPaginateVisitor.canPaginate(ClassType.of(Book.class), dictionary, scope, sparseFields));
    }
}
