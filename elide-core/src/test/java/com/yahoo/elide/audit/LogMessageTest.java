/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;

import com.yahoo.elide.core.RequestScope;
import com.google.common.collect.Sets;

import example.Child;
import example.Parent;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class LogMessageTest {
    private PersistentResource<Parent> parentRecord;
    private PersistentResource<Child> childRecord;
    private PersistentResource<Child> friendRecord;
    private EntityDictionary dictionary;

    @BeforeTest
    public void setup() {
        dictionary = new EntityDictionary();
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);

        Child child = new Child();
        child.setId(5);

        Parent parent = new Parent();
        parent.setId(7);

        Child friend = new Child();
        friend.setId(9);
        child.setFriends(Sets.newHashSet(friend));

        RequestScope requestScope = new RequestScope(null, null, null, dictionary, null);

        parentRecord = new PersistentResource<>(parent, requestScope);
        childRecord = new PersistentResource<>(parentRecord, child, requestScope);
        friendRecord = new PersistentResource<>(childRecord, friend, requestScope);
    }

    @Test
    public void verifyObectExpressions() throws Exception {
        String[] expressions = { "${child.id}", "${parent.getId()}" };

        LogMessage message = new LogMessage("{0} {1}", childRecord, expressions, 1);
        Assert.assertEquals("5 7", message.getMessage(), "JEXL substitution evaluates correctly.");
    }

    @Test
    public void verifyListExpressions() throws Exception {
        String[] expressions = { "${child[0].id}", "${child[1].id}", "${parent.getId()}" };

        LogMessage message = new LogMessage("{0} {1} {2}", friendRecord, expressions, 1);
        Assert.assertEquals("5 9 7", message.getMessage(), "JEXL substitution evaluates correctly.");
    }


    @Test(expectedExceptions = InvalidSyntaxException.class)
    public void invalidExpression() throws Exception {
        String[] expressions = { "${child.id}, ${%%%}" };

        LogMessage message = new LogMessage("{0} {1}", childRecord, expressions, 1);
    }

    @Test(expectedExceptions = InvalidSyntaxException.class)
    public void invalidTemplate() throws Exception {
        String[] expressions = { "${child.id}" };

        LogMessage message = new LogMessage("{}", childRecord, expressions, 1);
    }
}
