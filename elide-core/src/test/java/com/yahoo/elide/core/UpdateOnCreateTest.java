/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.User;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import example.Author;
import example.Book;
import example.UpdateAndCreate;
import org.mockito.Answers;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateOnCreateTest extends PersistenceResourceTestSetup {

    private User userOne;
    private User userTwo;
    private User userThree;

    private RequestScope userOneScope;
    private RequestScope userTwoScope;
    private RequestScope userThreeScope;

    private DataStoreTransaction tx;


    public UpdateOnCreateTest() {
        super();
    }

    @BeforeTest
    public void init() {
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(UpdateAndCreate.class);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        updateAndCreateNewObject.setId(1L);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();
        updateAndCreateExistingObject.setId(2L);
        Book book = new Book();
        Author author = new Author();

        tx = mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS);

        userOne = new User(1);
        userOneScope = new RequestScope(null, null, tx, userOne, null, elideSettings);
        userTwo = new User(2);
        userTwoScope = new RequestScope(null, null, tx, userTwo, null, elideSettings);
        userThree = new User(3);
        userThreeScope = new RequestScope(null, null, tx, userThree, null, elideSettings);

        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);
        when(tx.loadObject(eq(UpdateAndCreate.class),
                eq((Serializable) CoerceUtil.coerce(1, Long.class)),
                eq(Optional.empty()),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);
        when(tx.loadObject(eq(Book.class),
                eq((Serializable) CoerceUtil.coerce(1, Long.class)),
                eq(Optional.empty()),
                any(RequestScope.class)
        )).thenReturn(book);
        when(tx.loadObject(eq(Author.class),
                eq((Serializable) CoerceUtil.coerce(1, Long.class)),
                eq(Optional.empty()),
                any(RequestScope.class)
        )).thenReturn(author);
    }

    @Test
    public void testUpdateOnCreateUserOne() {

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userOneScope, Optional.of("uuid"));
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userOneScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userOneScope);

        created.updateAttribute("name", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            created.updateAttribute("type", UpdateAndCreate.AuthorType.CONTRACTED);
            created.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        created.updateAttribute("alias", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
        created.addRelation("books", loadedBook);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            created.addRelation("author", loadedAuthor);
            created.getRequestScope().getPermissionExecutor().executeCommitChecks();

        });
    }

    @Test
    public void testNormalUpdateUserOne() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userOneScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userOneScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userOneScope);

        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.updateAttribute("name", "");
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.updateAttribute("type", UpdateAndCreate.AuthorType.CONTRACTED);
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.updateAttribute("alias", "");
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.addRelation("books", loadedBook);
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.addRelation("author", loadedAuthor);
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();

        });
    }

    @Test
    public void testUpdateOnCreateUserTwo() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userTwoScope, Optional.of("uuid"));
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userTwoScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userTwoScope);

        created.updateAttribute("name", "");
        created.updateAttribute("type", UpdateAndCreate.AuthorType.CONTRACTED);
        created.updateAttribute("alias", "");
        created.addRelation("books", loadedBook);
        created.addRelation("author", loadedAuthor);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testNormalUpdateUserTwo() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userTwoScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userTwoScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userTwoScope);

        loaded.updateAttribute("name", "");
        loaded.updateAttribute("type", UpdateAndCreate.AuthorType.CONTRACTED);
        loaded.updateAttribute("alias", "");
        loaded.addRelation("books", loadedBook);
        loaded.addRelation("author", loadedAuthor);
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }


    @Test
    public void testUpdateOnCreateUserThree() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userThreeScope, Optional.of("uuid"));
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userThreeScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userThreeScope);

        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            created.updateAttribute("name", "");
            created.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            created.addRelation("books", loadedBook);
            created.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            created.updateAttribute("type", UpdateAndCreate.AuthorType.CONTRACTED);
            created.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            created.addRelation("author", loadedAuthor);
            created.getRequestScope().getPermissionExecutor().executeCommitChecks();

        });

        created.updateAttribute("alias", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();

    }

    @Test
    public void testNormalUpdateUserThree() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userThreeScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userThreeScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userThreeScope);

        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.updateAttribute("name", "");
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.updateAttribute("type", UpdateAndCreate.AuthorType.CONTRACTED);
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.updateAttribute("alias", "");
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.addRelation("books", loadedBook);
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
        });
        Assert.assertThrows(ForbiddenAccessException.class, () -> {
            loaded.addRelation("author", loadedAuthor);
            loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();

        });
    }
}
