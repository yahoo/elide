/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.TestLogger;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import com.yahoo.elide.optimization.Role;
import com.yahoo.elide.optimization.UserCheck;
import example.Child;
import example.NegativeIntegerUserCheck;
import example.Parent;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static com.yahoo.elide.optimization.UserCheck.ALLOW;
import static com.yahoo.elide.optimization.UserCheck.DENY;

public class UserTest {
    private final EntityDictionary dictionary = new EntityDictionary();
    private final Logger testLogger = new TestLogger();

    public static class CounterCheck implements UserCheck {
        public int callCounter = 0;

        @Override
        public UserPermission userPermission(User user) {
            callCounter++;
            return ALLOW;
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

        Assert.assertEquals(user.checkUserPermission(counterCheck), ALLOW, "The check should succeed");
        Assert.assertEquals(counterCheck.callCounter, 1, "The same check executed twice should only be evaluated once");
    }

    @Test
    public void testCheckOnlyCalledTwice() throws Exception {
        CounterCheck counterCheck = new CounterCheck();

        User user = new User(new Object());
        User user2 = new User(new Object());

        Assert.assertEquals(user.checkUserPermission(counterCheck), ALLOW, "The check should succeed");
        Assert.assertEquals(user2.checkUserPermission(counterCheck), ALLOW, "The check should succeed and not be cached");
        Assert.assertEquals(counterCheck.callCounter, 2, "The same check on two different objects should be evaluated twice.");
    }

    @Test
    public void testCheckType() {
        User goodUser = new User(1);
        Assert.assertEquals(goodUser.checkUserPermission(new Role.ALL()), ALLOW);
        Assert.assertEquals(goodUser.checkUserPermission(new Role.NONE()), DENY);
        Assert.assertEquals(goodUser.checkUserPermission(new NegativeIntegerUserCheck()), ALLOW);

        User badUser = new User(-1);
        Assert.assertEquals(badUser.checkUserPermission(new NegativeIntegerUserCheck()), DENY);
    }

    private RequestScope getScope(User user) {
        return new RequestScope(new JsonApiDocument(), null, user, dictionary, null, testLogger);
    }
}
