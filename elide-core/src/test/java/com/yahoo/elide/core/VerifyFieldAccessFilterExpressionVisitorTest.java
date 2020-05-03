/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.permissions.ExpressionResult;

import example.Author;
import example.Book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class VerifyFieldAccessFilterExpressionVisitorTest {

    private static final String AUTHORS = "authors";
    private static final String GENRE = "genre";
    private static final String HOME = "homeAddress";
    private static final String NAME = "name";
    private static final String SCIFI = "scifi";
    private RequestScope scope;

    @BeforeEach
    public void setupMocks() {
        // this will test with the default interface implementation
        scope = mock(RequestScope.class);
        PermissionExecutor permissionExecutor = mock(PermissionExecutor.class);
        DataStoreTransaction transaction = mock(DataStoreTransaction.class);
        EntityDictionary dictionary = new EntityDictionary(Collections.emptyMap());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);

        when(scope.getDictionary()).thenReturn(dictionary);
        when(scope.getPermissionExecutor()).thenReturn(permissionExecutor);
        when(scope.getTransaction()).thenReturn(transaction);
        when(permissionExecutor.evaluateFilterJoinUserChecks(any(), any())).thenCallRealMethod();
        when(permissionExecutor.handleFilterJoinReject(any(), any(), any())).thenCallRealMethod();
    }

    @Test
    public void testAccept() throws Exception {
        Path p1Path = new Path(Arrays.asList(
                new PathElement(Book.class, Author.class, AUTHORS),
                new PathElement(Author.class, String.class, NAME)
        ));
        FilterPredicate p1 = new InPredicate(p1Path, "foo", "bar");

        Path p2Path = new Path(Arrays.asList(
                new PathElement(Book.class, String.class, NAME)
        ));
        FilterPredicate p2 = new InPredicate(p2Path, "blah");

        Path p3Path = new Path(Arrays.asList(
                new PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p3 = new InPredicate(p3Path, SCIFI);

        //P4 is a duplicate of P3
        Path p4Path = new Path(Arrays.asList(
                new PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p4 = new InPredicate(p4Path, SCIFI);

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        Book book = new Book();
        Author author = new Author();
        book.setAuthors(Collections.singleton(author));
        author.setBooks(Collections.singleton(book));

        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // unrestricted fields
        assertTrue(not.accept(visitor));
        assertTrue(and1.accept(visitor));
        assertTrue(and2.accept(visitor));
        assertTrue(or.accept(visitor));
        assertTrue(p1.accept(visitor));
        assertTrue(p2.accept(visitor));
        assertTrue(p3.accept(visitor));
        assertTrue(p4.accept(visitor));

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();
        verify(permissionExecutor, times(17)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(5)).checkSpecificFieldPermissions(resource, null, ReadPermission.class, NAME);
        verify(permissionExecutor, times(21)).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, never()).handleFilterJoinReject(any(), any(), any());
    }

    @Test
    public void testReject() throws Exception {
        Path p1Path = new Path(Arrays.asList(
                new PathElement(Book.class, Author.class, AUTHORS),
                new PathElement(Author.class, String.class, NAME)
        ));
        FilterPredicate p1 = new InPredicate(p1Path, "foo", "bar");

        Path p2Path = new Path(Arrays.asList(
                new PathElement(Book.class, String.class, HOME)
        ));
        FilterPredicate p2 = new InPredicate(p2Path, "blah");

        Path p3Path = new Path(Arrays.asList(
                new PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p3 = new InPredicate(p3Path, SCIFI);

        //P4 is a duplicate of P3
        Path p4Path = new Path(Arrays.asList(
                new PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p4 = new InPredicate(p4Path, SCIFI);

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        Book book = new Book();
        Author author = new Author();
        book.setAuthors(Collections.singleton(author));
        author.setBooks(Collections.singleton(book));
        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();
        when(permissionExecutor.checkSpecificFieldPermissions(resource, null, ReadPermission.class, HOME))
            .thenThrow(ForbiddenAccessException.class);

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertFalse(not.accept(visitor));
        assertFalse(and1.accept(visitor));
        assertFalse(and2.accept(visitor));
        assertFalse(or.accept(visitor));
        assertFalse(p2.accept(visitor));

        // unrestricted fields
        assertTrue(p1.accept(visitor));
        assertTrue(p3.accept(visitor));
        assertTrue(p4.accept(visitor));

        verify(permissionExecutor, times(8)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(5)).checkSpecificFieldPermissions(resource, null, ReadPermission.class, HOME);
        verify(permissionExecutor, times(9)).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, times(5)).handleFilterJoinReject(any(), any(), any());
    }

    @Test
    public void testShortCircuitReject() throws Exception {
        RSQLFilterDialect dialect = new RSQLFilterDialect(scope.getDictionary());
        FilterExpression expression =
                dialect.parseFilterExpression("genre==foo", Book.class, true);

        Book book = new Book();
        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();

        when(permissionExecutor.checkUserPermissions(Book.class, ReadPermission.class, GENRE))
                .thenThrow(ForbiddenAccessException.class);

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertFalse(expression.accept(visitor));

        verify(permissionExecutor, times(1)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(1)).checkUserPermissions(Book.class, ReadPermission.class, GENRE);
        verify(permissionExecutor, never()).checkSpecificFieldPermissions(resource, null, ReadPermission.class, GENRE);
        verify(permissionExecutor, times(1)).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, times(1)).handleFilterJoinReject(any(), any(), any());
    }

    @Test
    public void testShortCircuitRejectDeferThenFail() throws Exception {
        RSQLFilterDialect dialect = new RSQLFilterDialect(scope.getDictionary());
        FilterExpression expression =
                dialect.parseFilterExpression("authors.homeAddress==main", Book.class, true);

        Book book = new Book();
        Author author = new Author();
        book.setAuthors(Collections.singleton(author));
        author.setBooks(Collections.singleton(book));

        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();
        DataStoreTransaction tx = scope.getTransaction();

        when(permissionExecutor.checkUserPermissions(Book.class, ReadPermission.class, AUTHORS))
                .thenReturn(ExpressionResult.DEFERRED);
        when(permissionExecutor.checkUserPermissions(Author.class, ReadPermission.class, HOME))
                .thenThrow(ForbiddenAccessException.class);

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertFalse(expression.accept(visitor));

        verify(permissionExecutor, times(1)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(1)).checkUserPermissions(Book.class, ReadPermission.class, AUTHORS);
        verify(permissionExecutor, never()).getReadPermissionFilter(Author.class);
        verify(permissionExecutor, times(1)).checkUserPermissions(Author.class, ReadPermission.class, HOME);
        verify(permissionExecutor, never()).checkSpecificFieldPermissions(any(), any(), any(), any());
        verify(permissionExecutor, never()).checkSpecificFieldPermissionsDeferred(any(), any(), any(), any());
        verify(permissionExecutor, times(2)).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, times(1)).handleFilterJoinReject(any(), any(), any());
        verify(tx, never()).getRelation(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testShortCircuitDeferred() throws Exception {
        RSQLFilterDialect dialect = new RSQLFilterDialect(scope.getDictionary());
        FilterExpression expression =
                dialect.parseFilterExpression("genre==foo", Book.class, true);

        Book book = new Book();
        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();

        when(permissionExecutor.checkUserPermissions(Book.class, ReadPermission.class, GENRE))
                .thenReturn(ExpressionResult.DEFERRED);
        when(permissionExecutor.checkSpecificFieldPermissions(resource, null, ReadPermission.class, GENRE))
                .thenThrow(ForbiddenAccessException.class);

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertFalse(expression.accept(visitor));

        verify(permissionExecutor, times(1)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(1)).checkUserPermissions(Book.class, ReadPermission.class, GENRE);
        verify(permissionExecutor, times(1)).checkSpecificFieldPermissions(resource, null, ReadPermission.class, GENRE);
        verify(permissionExecutor, times(1)).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, times(1)).handleFilterJoinReject(any(), any(), any());
    }

    @Test
    public void testShortCircuitPass() throws Exception {
        RSQLFilterDialect dialect = new RSQLFilterDialect(scope.getDictionary());
        FilterExpression expression =
                dialect.parseFilterExpression("authors.name==foo", Book.class, true);

        Book book = new Book();
        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();
        DataStoreTransaction tx = scope.getTransaction();

        when(permissionExecutor.checkUserPermissions(Book.class, ReadPermission.class, AUTHORS))
                .thenReturn(ExpressionResult.PASS);
        when(permissionExecutor.checkUserPermissions(Author.class, ReadPermission.class, NAME))
                .thenReturn(ExpressionResult.PASS);

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertTrue(expression.accept(visitor));

        verify(permissionExecutor, times(1)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(1)).checkUserPermissions(Book.class, ReadPermission.class, AUTHORS);
        verify(permissionExecutor, times(1)).checkUserPermissions(Author.class, ReadPermission.class, NAME);
        verify(permissionExecutor, never()).checkSpecificFieldPermissions(resource, null, ReadPermission.class, GENRE);
        verify(permissionExecutor, times(2)).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, never()).handleFilterJoinReject(any(), any(), any());
        verify(tx, never()).getRelation(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testUserChecksDeferred() throws Exception {
        RSQLFilterDialect dialect = new RSQLFilterDialect(scope.getDictionary());
        FilterExpression expression =
                dialect.parseFilterExpression("authors.homeAddress==main", Book.class, true);

        Book book = new Book();
        Author author = new Author();
        book.setAuthors(Collections.singleton(author));
        author.setBooks(Collections.singleton(book));

        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);
        PersistentResource<Author> resourceAuthor = new PersistentResource<>(author, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();
        DataStoreTransaction tx = scope.getTransaction();

        when(permissionExecutor.checkUserPermissions(Book.class, ReadPermission.class, AUTHORS))
                .thenReturn(ExpressionResult.PASS);
        when(permissionExecutor.checkSpecificFieldPermissionsDeferred(resource, null, ReadPermission.class, AUTHORS))
                .thenReturn(ExpressionResult.PASS);
        when(permissionExecutor.getReadPermissionFilter(Author.class)).thenReturn(Optional.empty());

        when(permissionExecutor.checkUserPermissions(Author.class, ReadPermission.class, HOME))
                .thenReturn(ExpressionResult.DEFERRED);
        when(permissionExecutor.checkSpecificFieldPermissions(resourceAuthor, null, ReadPermission.class, HOME))
                .thenThrow(ForbiddenAccessException.class);

        when(tx.getRelation(tx, book, AUTHORS, Optional.empty(), Optional.empty(), Optional.empty(), scope))
                .thenReturn(book.getAuthors());

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertFalse(expression.accept(visitor));

        verify(permissionExecutor, times(1)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(1)).checkUserPermissions(Book.class, ReadPermission.class, AUTHORS);
        verify(permissionExecutor, times(1)).getReadPermissionFilter(Author.class);
        verify(permissionExecutor, times(1)).checkUserPermissions(Author.class, ReadPermission.class, HOME);
        verify(permissionExecutor, times(1)).checkSpecificFieldPermissions(resourceAuthor, null, ReadPermission.class, HOME);
        verify(permissionExecutor, times(2)).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, times(1)).handleFilterJoinReject(any(), any(), any());
        verify(tx, times(1)).getRelation(tx, book, AUTHORS, Optional.empty(), Optional.empty(), Optional.empty(), scope);
    }

    @Test
    public void testBypassReadonlyFilterRestriction() throws Exception {
        RSQLFilterDialect dialect = new RSQLFilterDialect(scope.getDictionary());
        FilterExpression expression =
                dialect.parseFilterExpression("authors.name==foo", Book.class, true);

        Book book = new Book();
        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();
        DataStoreTransaction tx = scope.getTransaction();

        when(permissionExecutor.evaluateFilterJoinUserChecks(any(), any())).thenReturn(ExpressionResult.PASS);

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertTrue(expression.accept(visitor));

        verify(permissionExecutor, times(1)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, never()).checkSpecificFieldPermissions(any(), any(), any(), any());
        verify(permissionExecutor, never()).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, never()).handleFilterJoinReject(any(), any(), any());
        verify(tx, never()).getRelation(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testCustomFilterJoin() throws Exception {
        RSQLFilterDialect dialect = new RSQLFilterDialect(scope.getDictionary());
        FilterExpression expression =
                dialect.parseFilterExpression("genre==foo", Book.class, true);

        Book book = new Book();
        PersistentResource<Book> resource = new PersistentResource<>(book, null, "", scope);

        PermissionExecutor permissionExecutor = scope.getPermissionExecutor();
        DataStoreTransaction tx = scope.getTransaction();

        when(permissionExecutor.checkUserPermissions(Book.class, ReadPermission.class, GENRE))
                .thenReturn(ExpressionResult.DEFERRED);
        when(permissionExecutor.checkSpecificFieldPermissions(resource, null, ReadPermission.class, GENRE))
                .thenThrow(new ForbiddenAccessException("check expression"));

        when(permissionExecutor.evaluateFilterJoinUserChecks(any(), any())).thenReturn(ExpressionResult.DEFERRED);
        when(permissionExecutor.handleFilterJoinReject(any(), any(), any())).thenAnswer(invocation -> {
            FilterPredicate filterPredicate = invocation.getArgument(0);
            PathElement pathElement = invocation.getArgument(1);
            ForbiddenAccessException reason = invocation.getArgument(2);

            assertEquals("Book", pathElement.getType().getSimpleName());
            assertEquals(GENRE, filterPredicate.getField());
            assertEquals("book.genre IN_INSENSITIVE [foo]", filterPredicate.toString());

            // custom processing
            return "Book".equals(pathElement.getType().getSimpleName())
                    && filterPredicate.toString().matches("book.genre IN_INSENSITIVE \\[\\w+\\]")
                    && reason.getLoggedMessage().matches(".*Message=check expression.*")
                            ? ExpressionResult.DEFERRED
                            : ExpressionResult.FAIL;
        });

        VerifyFieldAccessFilterExpressionVisitor visitor = new VerifyFieldAccessFilterExpressionVisitor(resource);
        // restricted HOME field
        assertTrue(expression.accept(visitor));

        verify(permissionExecutor, times(1)).evaluateFilterJoinUserChecks(any(), any());
        verify(permissionExecutor, times(1)).checkSpecificFieldPermissions(resource, null, ReadPermission.class, GENRE);
        verify(permissionExecutor, never()).checkUserPermissions(any(), any(), any());
        verify(permissionExecutor, times(1)).handleFilterJoinReject(any(), any(), any());
        verify(tx, never()).getRelation(any(), any(), any(), any(), any(), any(), any());
    }
}
