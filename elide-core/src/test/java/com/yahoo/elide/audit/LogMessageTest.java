/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;

import com.google.common.collect.Sets;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import example.Child;
import example.Parent;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

public class LogMessageTest {
    private transient PersistentResource<Child> childRecord;
    private transient PersistentResource<Child> friendRecord;

    @BeforeTest
    public void setup() {
        final EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);

        final Child child = new Child();
        child.setId(5);

        final Parent parent = new Parent();
        parent.setId(7);

        final Child friend = new Child();
        friend.setId(9);
        child.setFriends(Sets.newHashSet(friend));

        final RequestScope requestScope = new RequestScope(
                null, null, null, null, dictionary, null, new TestAuditLogger(), null, null, new Elide.ElideSettings(10, 10), new MultipleFilterDialect(dictionary));

        final PersistentResource<Parent> parentRecord = new PersistentResource<>(parent, requestScope);
        childRecord = new PersistentResource<>(parentRecord, child, requestScope);
        friendRecord = new PersistentResource<>(childRecord, friend, requestScope);
    }

    @Test
    public void verifyObjectExpressions() {
        final String[] expressions = { "${child.id}", "${parent.getId()}" };
        final LogMessage message = new LogMessage("{0} {1}", childRecord, expressions, 1, Optional.empty());
        Assert.assertEquals("5 7", message.getMessage(), "JEXL substitution evaluates correctly.");
        Assert.assertEquals(message.getChangeSpec(), Optional.empty());
    }

    @Test
    public void verifyListExpressions() {
        final String[] expressions = { "${child[0].id}", "${child[1].id}", "${parent.getId()}" };
        final String[] expressionForDefault = { "${child.id}" };
        final LogMessage message = new LogMessage("{0} {1} {2}", friendRecord, expressions, 1, Optional.empty());
        final LogMessage defaultMessage = new LogMessage("{0}", friendRecord, expressionForDefault, 1, Optional.empty());
        Assert.assertEquals(message.getMessage(), "5 9 7", "JEXL substitution evaluates correctly.");
        Assert.assertEquals(defaultMessage.getMessage(), "9", "JEXL substitution evaluates correctly.");
        Assert.assertEquals(message.getChangeSpec(), Optional.empty());
    }


    @Test(expectedExceptions = InvalidSyntaxException.class)
    public void invalidExpression() {
        final String[] expressions = { "${child.id}, ${%%%}" };
        new LogMessage("{0} {1}", childRecord, expressions, 1, Optional.empty()).getMessage();
    }

    @Test(expectedExceptions = InvalidSyntaxException.class)
    public void invalidTemplate() {
        final String[] expressions = { "${child.id}" };
        new LogMessage("{}", childRecord, expressions, 1, Optional.empty()).getMessage();
    }

    public static class TestLoggerException extends RuntimeException {
    }

    private AuditLogger testAuditLogger = new Slf4jLogger();

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
            testAuditLogger.log(failMessage);
            Thread.sleep(Math.floorMod(new Random().nextInt(), 100));
            testAuditLogger.commit((RequestScope) null);
            Assert.fail("Exception expected");
        } catch (TestLoggerException e) {
            Assert.assertSame(e, testException);
        }

        // should not cause another exception
        try {
            testAuditLogger.commit((RequestScope) null);
        } catch (TestLoggerException e) {
            Assert.fail("Exception not cleared from previous logger commit");
        }
    }
}
