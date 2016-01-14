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

import java.io.IOException;
import java.util.Random;

public class LogMessageTest {
    private transient PersistentResource<Child> childRecord;
    private transient PersistentResource<Child> friendRecord;

    @BeforeTest
    public void setup() {
        final EntityDictionary dictionary = new EntityDictionary();
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);

        final Child child = new Child();
        child.setId(5);

        final Parent parent = new Parent();
        parent.setId(7);

        final Child friend = new Child();
        friend.setId(9);
        child.setFriends(Sets.newHashSet(friend));

        final RequestScope requestScope = new RequestScope(null, null, null, dictionary, null, new TestLogger());

        final PersistentResource<Parent> parentRecord = new PersistentResource<>(parent, requestScope);
        childRecord = new PersistentResource<>(parentRecord, child, requestScope);
        friendRecord = new PersistentResource<>(childRecord, friend, requestScope);
    }

    @Test
    public void verifyObjectExpressions() {
        final String[] expressions = { "${child.id}", "${parent.getId()}" };
        final LogMessage message = new LogMessage("{0} {1}", childRecord, expressions, 1);
        Assert.assertEquals("5 7", message.getMessage(), "JEXL substitution evaluates correctly.");
    }

    @Test
    public void verifyListExpressions() {
        final String[] expressions = { "${child[0].id}", "${child[1].id}", "${parent.getId()}" };
        final LogMessage message = new LogMessage("{0} {1} {2}", friendRecord, expressions, 1);
        Assert.assertEquals("5 9 7", message.getMessage(), "JEXL substitution evaluates correctly.");
    }


    @Test(expectedExceptions = InvalidSyntaxException.class)
    public void invalidExpression() {
        final String[] expressions = { "${child.id}, ${%%%}" };
        new LogMessage("{0} {1}", childRecord, expressions, 1).getMessage();
    }

    @Test(expectedExceptions = InvalidSyntaxException.class)
    public void invalidTemplate() {
        final String[] expressions = { "${child.id}" };
        new LogMessage("{}", childRecord, expressions, 1).getMessage();
    }

    public static class TestLoggerException extends RuntimeException {
    }

    private Logger testLogger = new Slf4jLogger();

    @Test(threadPoolSize = 10, invocationCount = 10)
    public void threadSafeLogger() throws IOException, InterruptedException {
        TestLoggerException testException = new TestLoggerException();
        LogMessage failMessage = new LogMessage("test", 0) {
            @Override
            public String getMessage() {
                throw testException;
            }
        };
        try {
            testLogger.log(failMessage);
            Thread.sleep(Math.floorMod(new Random().nextInt(), 100));
            testLogger.commit();
            Assert.fail("Exception expected");
        } catch (TestLoggerException e) {
            Assert.assertSame(e, testException);
        }

        // should not cause another exception
        try {
            testLogger.commit();
        } catch (TestLoggerException e) {
            Assert.fail("Exception not cleared from previous logger commit");
        }
    }
}
