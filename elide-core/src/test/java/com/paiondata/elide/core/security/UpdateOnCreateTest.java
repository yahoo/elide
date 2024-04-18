/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.paiondata.elide.core.PersistenceResourceTestSetup;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.TestRequestScope;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.type.ClassType;
import example.Author;
import example.Book;
import example.UpdateAndCreate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class UpdateOnCreateTest extends PersistenceResourceTestSetup {

    private User userOne = new TestUser("1");
    private User userTwo = new TestUser("2");
    private User userThree = new TestUser("3");
    private User userFour = new TestUser("4");

    private DataStoreTransaction tx = mock(DataStoreTransaction.class);

    @BeforeEach
    public void beforeMethod() {
        reset(tx);
    }

    public UpdateOnCreateTest() {
        super();
        initDictionary();
    }

    //----------------------------------------- ** Entity Creation ** -------------------------------------------------

    //Create allowed based on class level expression
    @Test
    public void createPermissionCheckClassAnnotationForCreatingAnEntitySuccessCase() {
        com.paiondata.elide.core.RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userOneScope)).thenReturn(updateAndCreateNewObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userOneScope, Optional.of("1"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    //Create allowed based on field level expression
    @Test
    public void createPermissionCheckFieldAnnotationForCreatingAnEntitySuccessCase() {
        com.paiondata.elide.core.RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userThreeScope)).thenReturn(updateAndCreateNewObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userThreeScope, Optional.of("2"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    //Create denied based on field level expression
    @Test
    public void createPermissionCheckFieldAnnotationForCreatingAnEntityFailureCase() {
        com.paiondata.elide.core.RequestScope userFourScope = new TestRequestScope(tx, userFour, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userFourScope)).thenReturn(updateAndCreateNewObject);
        assertThrows(
                ForbiddenAccessException.class,
                () -> com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userFourScope, Optional.of("3")));
    }

    //----------------------------------------- ** Update Attribute ** ------------------------------------------------
    //Expression for field inherited from class level expression
    @Test
    public void updatePermissionInheritedForAttributeSuccessCase() {
        com.paiondata.elide.core.RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)
                        .build(),
                "1",
                userTwoScope);
        loaded.updateAttribute("name", "");
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void updatePermissionInheritedForAttributeFailureCase() {
        com.paiondata.elide.core.RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)
                        .build(),
                "1",
                userOneScope);
        assertThrows(ForbiddenAccessException.class, () -> loaded.updateAttribute("name", ""));
    }

    //Class level expression overwritten by field level expression
    @Test
    public void updatePermissionOverwrittenForAttributeSuccessCase() {
        com.paiondata.elide.core.RequestScope userFourScope = new TestRequestScope(tx, userFour, dictionary);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)
                        .build(),
                "1",
                userFourScope);
        loaded.updateAttribute("alias", "");
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void updatePermissionOverwrittenForAttributeFailureCase() {
        com.paiondata.elide.core.RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)
                        .build(),
                "1",
                userThreeScope);
        assertThrows(ForbiddenAccessException.class, () -> loaded.updateAttribute("alias", ""));
    }


    //----------------------------------------- ** Update Relation  ** -----------------------------------------------
    //Expression for relation inherited from class level expression
    @Test
    public void updatePermissionInheritedForRelationSuccessCase() {
        com.paiondata.elide.core.RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(new Book());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)
                        .build(),
                "1",
                userTwoScope);
        com.paiondata.elide.core.PersistentResource<Book> loadedBook = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(Book.class)
                        .build(),
                "2",
                userTwoScope);
        loaded.addRelation("books", loadedBook);
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void updatePermissionInheritedForRelationFailureCase() {
        com.paiondata.elide.core.RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(new Book());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)
                        .build(),
                "1",
                userOneScope);
        com.paiondata.elide.core.PersistentResource<Book> loadedBook = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(Book.class)
                        .build(),
                "2",
                userOneScope);
        assertThrows(ForbiddenAccessException.class, () -> loaded.addRelation("books", loadedBook));
    }

    //Class level expression overwritten by field level expression
    @Test
    public void updatePermissionOverwrittenForRelationSuccessCase() {
        com.paiondata.elide.core.RequestScope userThreeScope = new TestRequestScope(tx, new TestUser("3"), dictionary);


        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();
        updateAndCreateExistingObject.setId(1L);

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(new Author());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)
                        .build(),
                "1",
                userThreeScope);
        com.paiondata.elide.core.PersistentResource<Author> loadedAuthor = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(Author.class)
                        .build(),
                "2",
                userThreeScope);
        loaded.addRelation("author", loadedAuthor);
        loaded.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void updatePermissionOverwrittenForRelationFailureCase() {
        com.paiondata.elide.core.RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary);

        UpdateAndCreate updateAndCreateExistingObject = new UpdateAndCreate();

        when(tx.loadObject(any(),
                eq(1L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(updateAndCreateExistingObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(new Author());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> loaded = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(UpdateAndCreate.class)

                        .build(),
                "1",
                userTwoScope);
        com.paiondata.elide.core.PersistentResource<Author> loadedAuthor = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(Author.class)
                        .build(),
                "2",
                userTwoScope);
        assertThrows(ForbiddenAccessException.class, () -> loaded.addRelation("author", loadedAuthor));
    }

    //----------------------------------------- ** Update Attribute On Create ** --------------------------------------
    //Expression for field inherited from class level expression
    @Test
    public void createPermissionInheritedForAttributeSuccessCase() {
        com.paiondata.elide.core.RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userOneScope)).thenReturn(updateAndCreateNewObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userOneScope, Optional.of("4"));
        created.updateAttribute("name", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void createPermissionInheritedForAttributeFailureCase() {
        com.paiondata.elide.core.RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userThreeScope)).thenReturn(updateAndCreateNewObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userThreeScope, Optional.of("5"));
        assertThrows(ForbiddenAccessException.class, () -> created.updateAttribute("name", ""));
    }

    //Class level expression overwritten by field level expression
    @Test
    public void createPermissionOverwrittenForAttributeSuccessCase() {
        com.paiondata.elide.core.RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userThreeScope)).thenReturn(updateAndCreateNewObject);

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userThreeScope, Optional.of("6"));
        created.updateAttribute("alias", "");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void createPermissionOverwrittenForAttributeFailureCase() {
        com.paiondata.elide.core.RequestScope userFourScope = new TestRequestScope(tx, userFour, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userFourScope)).thenReturn(updateAndCreateNewObject);
        assertThrows(
                ForbiddenAccessException.class, () -> {
                    com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created =
                            com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userFourScope, Optional.of("7"));
                    created.updateAttribute("alias", "");
                }
        );
    }

    //----------------------------------------- ** Update Relation On Create ** --------------------------------------
    //Expression for relation inherited from class level expression
    @Test
    public void createPermissionInheritedForRelationSuccessCase() {
        com.paiondata.elide.core.RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userOneScope)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(new Book());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userOneScope, Optional.of("8"));
        com.paiondata.elide.core.PersistentResource<Book> loadedBook = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(Book.class)
                        .build(),
                "2",
                userOneScope);

        created.addRelation("books", loadedBook);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void createPermissionInheritedForRelationFailureCase() {
        com.paiondata.elide.core.RequestScope userThreeScope = new TestRequestScope(tx, userThree, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userThreeScope)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(new Book());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userThreeScope, Optional.of("9"));
        com.paiondata.elide.core.PersistentResource<Book> loadedBook = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                    .type(Book.class)
                    .build(),
                "2",
                userThreeScope);
        assertThrows(ForbiddenAccessException.class, () -> created.addRelation("books", loadedBook));
    }

    //Class level expression overwritten by field level expression
    @Test
    public void createPermissionOverwrittenForRelationSuccessCase() {
        com.paiondata.elide.core.RequestScope userTwoScope = new TestRequestScope(tx, userTwo, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userTwoScope)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(com.paiondata.elide.core.RequestScope.class)
        )).thenReturn(new Author());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userTwoScope, Optional.of("10"));
        com.paiondata.elide.core.PersistentResource<Author> loadedAuthor = com.paiondata.elide.core.PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(Author.class)
                        .build(),
                "2",
                userTwoScope);
        created.addRelation("author", loadedAuthor);
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void createPermissionOverwrittenForRelationFailureCase() {
        com.paiondata.elide.core.RequestScope userOneScope = new TestRequestScope(tx, userOne, dictionary);

        UpdateAndCreate updateAndCreateNewObject = new UpdateAndCreate();
        when(tx.createNewObject(ClassType.of(UpdateAndCreate.class), userOneScope)).thenReturn(updateAndCreateNewObject);

        when(tx.loadObject(any(),
                eq(2L),
                any(RequestScope.class)
        )).thenReturn(new Author());

        com.paiondata.elide.core.PersistentResource<UpdateAndCreate> created = com.paiondata.elide.core.PersistentResource.createObject(ClassType.of(UpdateAndCreate.class), userOneScope, Optional.of("11"));
        com.paiondata.elide.core.PersistentResource<Author> loadedAuthor = PersistentResource.loadRecord(
                EntityProjection.builder()
                        .type(Author.class)
                        .build(),
                "2",
                userOneScope);
        assertThrows(ForbiddenAccessException.class, () -> created.addRelation("author", loadedAuthor));
    }
}
