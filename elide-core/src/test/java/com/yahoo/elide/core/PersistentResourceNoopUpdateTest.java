/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.security.User;
import example.Child;
import example.FunWithPermissions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class PersistentResourceNoopUpdateTest extends PersistenceResourceTestSetup {
    private final RequestScope goodUserScope;
    PersistentResourceNoopUpdateTest() {
        goodUserScope = new RequestScope(null, null, null, mock(DataStoreTransaction.class),
                new User(1), null, elideSettings);
        initDictionary();
        reset(goodUserScope.getTransaction());
    }
    @Test
    public void testNOOPToOneAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, null, null, tx, goodUser, null, elideSettings);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        //We do not want the update to one method to be called when we add the existing entity to the relation
        funResource.addRelation("relation3", childResource);

        verify(tx, never()).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
    }

    @Test
    public void testToOneAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, null, null, tx, goodUser, null, elideSettings);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        funResource.addRelation("relation3", childResource);

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
    }

    @Test
    public void testNOOPToManyAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        Set<Child> children = new HashSet<>();
        children.add(child);
        fun.setRelation1(children);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, null, null, tx, goodUser, null, elideSettings);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, null, goodScope);
        //We do not want the update to one method to be called when we add the existing entity to the relation
        funResource.addRelation("relation1", childResource);
        verify(tx, never()).updateToManyRelation(eq(tx), eq(child), eq("relation1"), any(), any(), eq(goodScope));
    }

    @Test
    public void testToManyAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, null, null, tx, goodUser, null, elideSettings);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, null, goodScope);
        funResource.addRelation("relation1", childResource);
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(fun), eq("relation1"), any(), any(), eq(goodScope));
    }
}
