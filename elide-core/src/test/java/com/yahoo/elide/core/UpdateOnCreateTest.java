/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.User;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import example.UpdateAndCreate;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Optional;

public class UpdateOnCreateTest extends PersistenceResourceTestSetup {

    private RequestScope userOneScope;
    private RequestScope userTwoScope;
    private RequestScope userThreeScope;
    private RequestScope userFourScope;

    @BeforeTest
    public void init() {
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(UpdateAndCreate.class);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        updateAndCreateNewObject.setId(1L);
        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();
        updateAndCreateExistingObject.setId(2L);
        Book book = new Book();
        Author author = new Author();
        Publisher publisher = new Publisher();
        Editor editor = new Editor();

        publisher.setEditor(editor);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        User userOne = new User(1);
        userOneScope = new RequestScope(null, null, tx, userOne, null, elideSettings, false);
        User userTwo = new User(2);
        userTwoScope = new RequestScope(null, null, tx, userTwo, null, elideSettings, false);
        User userThree = new User(3);
        userThreeScope = new RequestScope(null, null, tx, userThree, null, elideSettings, false);
        User userFour = new User(4);
        userFourScope = new RequestScope(null, null, tx, userFour, null, elideSettings, false);

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
        when(tx.loadObject(eq(Publisher.class),
                eq((Serializable) CoerceUtil.coerce(1, Long.class)),
                eq(Optional.empty()),
                any(RequestScope.class)
        )).thenReturn(publisher);
    }

    //----------------------------------------- ** Entity Creation ** -------------------------------------------------

    //Create allowed based on class level expression
    @Test
    public void createPermissionCheckClassAnnotationForCreatingAnEntitySuccessCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userOneScope, Optional.of("1"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    //Create allowed based on field level expression
    @Test
    public void createPermissionCheckFieldAnnotationForCreatingAnEntitySuccessCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userThreeScope, Optional.of("2"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    //Create denied based on field level expression
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionCheckFieldAnnotationForCreatingAnEntityFailureCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userFourScope, Optional.of("3"));
    }

    //----------------------------------------- ** Update Attribute ** ------------------------------------------------
    //Expression for field inherited from class level expression
    @Test
    public void updatePermissionInheritedForAttributeSuccessCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userTwoScope);
        loaded.updateAttribute("name", "");
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionInheritedForAttributeFailureCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userOneScope);
        loaded.updateAttribute("name", "");
    }

    //Class level expression overwritten by field level expression
    @Test
    public void updatePermissionOverwrittenForAttributeSuccessCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userFourScope);
        loaded.updateAttribute("alias", "");
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionOverwrittenForAttributeFailureCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userThreeScope);
        loaded.updateAttribute("alias", "");
    }


    //----------------------------------------- ** Update Relation  ** -----------------------------------------------
    //Expression for relation inherited from class level expression
    @Test
    public void updatePermissionInheritedForRelationSuccessCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userTwoScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userTwoScope);
        loaded.addRelation("books", loadedBook);
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionInheritedForRelationFailureCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userOneScope);
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userOneScope);
        loaded.addRelation("books", loadedBook);
    }

    //Class level expression overwritten by field level expression
    @Test
    public void updatePermissionOverwrittenForRelationSuccessCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userThreeScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userThreeScope);
        loaded.addRelation("author", loadedAuthor);
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void updatePermissionOverwrittenForRelationFailureCase() {
        PersistentResource<UpdateAndCreate> loaded = PersistentResource.loadRecord(UpdateAndCreate.class,
                "1",
                userTwoScope);
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userTwoScope);
        loaded.addRelation("author", loadedAuthor);
    }

    //----------------------------------------- ** Update Attribute On Create ** --------------------------------------
    //Expression for field inherited from class level expression
    @Test
    public void createPermissionInheritedForAttributeSuccessCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userOneScope, Optional.of("4"));
        created.updateAttribute("name", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionInheritedForAttributeFailureCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userThreeScope, Optional.of("5"));
        created.updateAttribute("name", "");
    }

    //Class level expression overwritten by field level expression
    @Test
    public void createPermissionOverwrittenForAttributeSuccessCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userThreeScope, Optional.of("6"));
        created.updateAttribute("alias", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionOverwrittenForAttributeFailureCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userFourScope, Optional.of("7"));
        created.updateAttribute("alias", "");
    }


    //----------------------------------------- ** Update Relation On Create ** --------------------------------------
    //Expression for relation inherited from class level expression
    @Test
    public void createPermissionInheritedForRelationSuccessCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userOneScope, Optional.of("8"));
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userOneScope);
        created.addRelation("books", loadedBook);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionInheritedForRelationFailureCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userThreeScope, Optional.of("9"));
        PersistentResource<Book> loadedBook = PersistentResource.loadRecord(Book.class,
                "1",
                userThreeScope);
        created.addRelation("books", loadedBook);
    }

    //Class level expression overwritten by field level expression
    @Test
    public void createPermissionOverwrittenForRelationSuccessCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userTwoScope, Optional.of("10"));
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userTwoScope);
        created.addRelation("author", loadedAuthor);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void createPermissionOverwrittenForRelationFailureCase() {
        PersistentResource<UpdateAndCreate> created = PersistentResource.createObject(null, UpdateAndCreate.class, userOneScope, Optional.of("11"));
        PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(Author.class,
                "1",
                userOneScope);
        created.addRelation("author", loadedAuthor);
    }
}
