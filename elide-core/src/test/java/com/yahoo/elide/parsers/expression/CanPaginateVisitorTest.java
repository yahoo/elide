/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.UserCheck;

import com.google.common.collect.Sets;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;

public class CanPaginateVisitorTest {

    private Map<String, Class<? extends Check>> checkMappings;

    public static final class TestOperationCheck extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object,
                          com.yahoo.elide.security.RequestScope requestScope,
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
        public FilterExpression getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
            return null;
        }
    }


    @BeforeSuite
    void init() {
        checkMappings = new HashMap<>();
        checkMappings.put("In Memory Check", TestOperationCheck.class);
        checkMappings.put("False User Check", FalseUserCheck.class);
        checkMappings.put("True User Check", TrueUserCheck.class);
        checkMappings.put("Filter Expression Check", TestFilterExpressionCheck.class);
    }


    @Test
    public void testNoPermissions() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);

        RequestScope scope = mock(RequestScope.class);

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testClassOperationPermissions() throws Exception {
        @Entity
        @Include
        @ReadPermission(expression = "In Memory Check")
        class Book {
            @Id
            private long id;
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertFalse(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testClassUserPermissions() throws Exception {
        @Entity
        @Include
        @ReadPermission(expression = "False User Check")
        class Book {
            @Id
            private long id;
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testFieldFilterPermissions() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "Filter Expression Check")
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testComplexTrueExpression() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression =
                    "(Filter Expression Check AND False User Check) OR (Filter Expression Check OR NOT False User Check)")
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testFalseUserOROperationExpression() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "False User Check OR In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertFalse(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testTrueUserOROperationExpression() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "True User Check OR In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testFalseUserAndOperationExpression() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "False User Check AND In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testTrueUserAndOperationExpression() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "True User Check AND In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertFalse(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testNotOperationExpression() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "NOT In Memory Check")
            private String title;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertFalse(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testMultipleFieldsNoPagination() throws Exception {
        @Entity
        @Include
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "Filter Expression Check")
            private String title;

            @ReadPermission(expression = "In Memory Check")
            private Date publicationDate;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertFalse(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testMultipleFieldsPagination() throws Exception {
        @Entity
        @Include
        @ReadPermission(expression = "In Memory Check")
        class Book {
            @Id
            private long id;

            @ReadPermission(expression = "Filter Expression Check")
            private String title;

            @ReadPermission(expression = "Filter Expression Check")
            private Date publicationDate;
        }

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }

    @Test
    public void testSparseFields() throws Exception {
        @Entity
        @Include
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

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(Book.class);
        RequestScope scope = mock(RequestScope.class);

        Map<String, Set<String>> sparseFields = new HashMap<>();
        when(scope.getSparseFields()).thenReturn(sparseFields);

        Assert.assertFalse(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));

        sparseFields.put("book", Sets.newHashSet("title", "publicationDate"));

        Assert.assertTrue(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));

        sparseFields.put("book", Sets.newHashSet("outOfPrint"));

        Assert.assertFalse(CanPaginateVisitor.canPaginate(Book.class, dictionary, scope));
    }
}
