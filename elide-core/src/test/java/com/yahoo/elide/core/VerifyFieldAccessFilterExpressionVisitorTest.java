/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.security.PermissionExecutor;

import com.google.common.collect.Sets;

import example.Author;
import example.Book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class VerifyFieldAccessFilterExpressionVisitorTest {

    private static final String ENTITY = "entity";
    private static final String GENRE = "genre";
    private static final String HOME = "homeAddress";
    private static final String NAME = "name";
    private static final String SCIFI = "scifi";
    private RequestScope scope;

    @BeforeEach
    public void setupMocks() {
        // this will test with the default interface implementation
        scope = mock(RequestScope.class);
        EntityDictionary dictionary = mock(EntityDictionary.class);
        PermissionExecutor permissionExecutor = mock(PermissionExecutor.class);

        when(scope.getDictionary()).thenReturn(dictionary);
        when(dictionary.getIdType(String.class)).thenReturn((Class) Long.class);
        when(dictionary.getValue(ENTITY, NAME, scope)).thenReturn(3L);
        when(scope.getPermissionExecutor()).thenReturn(permissionExecutor);
    }

    @Test
    public void testAccept() throws Exception {
        Path p1Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, Author.class, "authors"),
                new Path.PathElement(Author.class, String.class, NAME)
        ));
        FilterPredicate p1 = new InPredicate(p1Path, "foo", "bar");

        Path p2Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, NAME)
        ));
        FilterPredicate p2 = new InPredicate(p2Path, "blah");

        Path p3Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p3 = new InPredicate(p3Path, SCIFI);

        //P4 is a duplicate of P3
        Path p4Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p4 = new InPredicate(p4Path, SCIFI);

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        Book book = new Book();
        Author author = new Author();
        book.setAuthors(Sets.newHashSet(author));
        author.setBooks(Sets.newHashSet(book));

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
        verify(permissionExecutor, times(5)).checkSpecificFieldPermissions(resource, null, ReadPermission.class, NAME);
    }

    @Test
    public void testReject() throws Exception {
        Path p1Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, Author.class, "authors"),
                new Path.PathElement(Author.class, String.class, NAME)
        ));
        FilterPredicate p1 = new InPredicate(p1Path, "foo", "bar");

        Path p2Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, HOME)
        ));
        FilterPredicate p2 = new InPredicate(p2Path, "blah");

        Path p3Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p3 = new InPredicate(p3Path, SCIFI);

        //P4 is a duplicate of P3
        Path p4Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, GENRE)
        ));
        FilterPredicate p4 = new InPredicate(p4Path, SCIFI);

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        Book book = new Book();
        Author author = new Author();
        book.setAuthors(Sets.newHashSet(author));
        author.setBooks(Sets.newHashSet(book));
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

        verify(permissionExecutor, times(5)).checkSpecificFieldPermissions(resource, null, ReadPermission.class, HOME);
    }
}
