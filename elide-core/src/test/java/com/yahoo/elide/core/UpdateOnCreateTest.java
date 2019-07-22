/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.User;

import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import example.UpdateAndCreate;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Optional;

public class UpdateOnCreateTest extends PersistenceResourceTestSetup {

    private User userOne = new User(1);
    private User userTwo = new User(2);
    private User userThree = new User(3);
    private User userFour = new User(4);

    private DataStoreTransaction tx = mock(DataStoreTransaction.class);

    @BeforeMethod
    public void beforeMethod() {
        reset(tx);
    }

    @BeforeTest
    public void init() {
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(UpdateAndCreate.class);
    }

    //----------------------------------------- ** Entity Creation ** -------------------------------------------------

    //Create allowed based on class level expression
    @Test
    public void createPermissionCheckClassAnnotationForCreatingAnEntitySuccessCase() {
        RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userOneScope, Optional.of("1"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    //Create allowed based on field level expression
    @Test
    public void createPermissionCheckFieldAnnotationForCreatingAnEntitySuccessCase() {
        RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userThreeScope, Optional.of("2"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    //Create denied based on field level expression
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionCheckFieldAnnotationForCreatingAnEntityFailureCase() {
        RequestScope userFourScope = new TestRequestScope(tx, userFour, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userFourScope, Optional.of("3"));
    }

    //----------------------------------------- ** Update Attribute ** ------------------------------------------------
    //Expression for field inherited from class level expression
    @Test
    public void updatePermissionInheritedForAttributeSuccessCase() {
        RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userTwoScope);
        loaded.updateAttribute("name", "");
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionInheritedForAttributeFailureCase() {
        RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userOneScope);
        loaded.updateAttribute("name", "");
    }

    //Class level expression overwritten by field level expression
    @Test
    public void updatePermissionOverwrittenForAttributeSuccessCase() {
        RequestScope userFourScope = new TestRequestScope(tx, userFour, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userFourScope);
        loaded.updateAttribute("alias", "");
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionOverwrittenForAttributeFailureCase() {
        RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userThreeScope);
        loaded.updateAttribute("alias", "");
    }


    //----------------------------------------- ** Update Relation  ** -----------------------------------------------
    //Expression for relation inherited from class level expression
    @Test
    public void updatePermissionInheritedForRelationSuccessCase() {
        RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Book());

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userTwoScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "2",
                userTwoScope);
        loaded.addRelation("books", loadedBook);
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionInheritedForRelationFailureCase() {
        RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Book());

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userOneScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "2",
                userOneScope);
        loaded.addRelation("books", loadedBook);
    }

    //Class level expression overwritten by field level expression
    @Test
    public void updatePermissionOverwrittenForRelationSuccessCase() {
        RequestScope userThreeScope = new TestRequestScope(tx, new User(3), dictionary,
                UpdateAndCreate.class, 1);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();
        updateAndCreateExistingObject.setId(1L);

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Author());

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userThreeScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "2",
                userThreeScope);
        loaded.addRelation("author", loadedAuthor);
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionOverwrittenForRelationFailureCase() {
        RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Author());

        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userTwoScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "2",
                userTwoScope);
        loaded.addRelation("author", loadedAuthor);
    }

    //----------------------------------------- ** Update Attribute On Create ** --------------------------------------
    //Expression for field inherited from class level expression
    @Test
    public void createPermissionInheritedForAttributeSuccessCase() {
        RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userOneScope, Optional.of("4"));
        created.updateAttribute("name", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionInheritedForAttributeFailureCase() {
        RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userThreeScope, Optional.of("5"));
        created.updateAttribute("name", "");
    }

    //Class level expression overwritten by field level expression
    @Test
    public void createPermissionOverwrittenForAttributeSuccessCase() {
        RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userThreeScope, Optional.of("6"));
        created.updateAttribute("alias", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionOverwrittenForAttributeFailureCase() {
        RequestScope userFourScope = new TestRequestScope(tx, userFour, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userFourScope, Optional.of("7"));
        created.updateAttribute("alias", "");
    }


    //----------------------------------------- ** Update Relation On Create ** --------------------------------------
    //Expression for relation inherited from class level expression
    @Test
    public void createPermissionInheritedForRelationSuccessCase() {
        RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Book());

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userOneScope, Optional.of("8"));
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "2",
                userOneScope);

        created.addRelation("books", loadedBook);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionInheritedForRelationFailureCase() {
        RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Book());

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userThreeScope, Optional.of("9"));
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(userThreeScope.getEntityProjection().getRelationship("books"),
                "2",
                userThreeScope);
        created.addRelation("books", loadedBook);
    }

    //Class level expression overwritten by field level expression
    @Test
    public void createPermissionOverwrittenForRelationSuccessCase() {
        RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Author());

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userTwoScope, Optional.of("10"));
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "2",
                userTwoScope);
        created.addRelation("author", loadedAuthor);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionOverwrittenForRelationFailureCase() {
        RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary, UpdateAndCreate.class, 1);
        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(UpdateAndCreate.class)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Author());

        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(UpdateAndCreate.class, userOneScope, Optional.of("11"));
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "2",
                userOneScope);
        created.addRelation("author", loadedAuthor);
    }
}
