/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import example.Child;
import example.Parent;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class UserTest {
    private EntityDictionary dictionary = new EntityDictionary();

    public class CounterCheck implements Check {
        public int callCounter = 0;
        @Override
        public boolean ok(PersistentResource resource) {
            callCounter++;
            return true;
        }
    }

    @BeforeTest
    void init() {
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);
    }

    @Test
    public void testCheckOnlyCalledOnce() throws Exception {
        CounterCheck counterCheck = new CounterCheck();

        User user = new User(new Object());

        Child child = new Child();
        Assert.assertTrue(user.ok(counterCheck, new PersistentResource(child, getScope(user))), "The check should succeed");
        Assert.assertEquals(counterCheck.callCounter, 1, "The same check executed twice should only be evaluated once");
    }

    @Test
    public void testCheckOnlyCalledTwice() throws Exception {
        CounterCheck counterCheck = new CounterCheck();

        User user = new User(new Object());

        Assert.assertTrue(user.ok(counterCheck, new PersistentResource(new Child(), getScope(user))), "The check should succeed");
        Assert.assertTrue(user.ok(counterCheck, new PersistentResource(new Parent(), getScope(user))), "The check should succeed");
        Assert.assertEquals(counterCheck.callCounter, 2, "The same check on two different objects should be evaluated twice.");
    }

    private RequestScope getScope(User user) {
        return new RequestScope(new JsonApiDocument(), null, user, dictionary, null);
    }
}
