/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.audit;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.TestUser;
import com.google.common.collect.Sets;
import example.Child;
import example.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class LogMessageImplTest {
    private static transient PersistentResource<Child> childRecord;
    private static transient PersistentResource<Child> friendRecord;

    @BeforeAll
    public static void init() {
        final EntityDictionary dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);

        final Child child = new Child();
        child.setId(5);

        final Parent parent = new Parent();
        parent.setId(7);

        final Child friend = new Child();
        friend.setId(9);
        child.setFriends(Sets.newHashSet(friend));

        final RequestScope requestScope = new RequestScope(null, null, NO_VERSION, null, null,
                new TestUser("aaron"), null, null,
                UUID.randomUUID(),
                new ElideSettingsBuilder(null)
                        .withAuditLogger(new TestAuditLogger())
                        .withEntityDictionary(dictionary)
                        .build());

        final PersistentResource<Parent> parentRecord = new PersistentResource<>(parent, requestScope.getUUIDFor(parent), requestScope);
        childRecord = new PersistentResource<>(child, parentRecord, "children", requestScope.getUUIDFor(child), requestScope);
        friendRecord = new PersistentResource<>(friend, childRecord, "friends", requestScope.getUUIDFor(friend), requestScope);
    }

    @Test
    public void verifyOpaqueUserExpressions() {
        final String[] expressions = { "${opaqueUser.name}", "${opaqueUser.name}" };
        final LogMessageImpl message = new LogMessageImpl("{0} {1}", childRecord, expressions, 1, Optional.empty());
        assertEquals("aaron aaron", message.getMessage(), "JEXL substitution evaluates correctly.");
        assertEquals(Optional.empty(), message.getChangeSpec());
    }

    @Test
    public void verifyObjectExpressions() {
        final String[] expressions = { "${child.id}", "${parent.getId()}" };
        final LogMessageImpl message = new LogMessageImpl("{0} {1}", childRecord, expressions, 1, Optional.empty());
        assertEquals("5 7", message.getMessage(), "JEXL substitution evaluates correctly.");
        assertEquals(Optional.empty(), message.getChangeSpec());
    }

    @Test
    public void verifyListExpressions() {
        final String[] expressions = { "${child[0].id}", "${child[1].id}", "${parent.getId()}" };
        final String[] expressionForDefault = { "${child.id}" };
        final LogMessageImpl message = new LogMessageImpl("{0} {1} {2}", friendRecord, expressions, 1, Optional.empty());
        final LogMessageImpl defaultMessage = new LogMessageImpl("{0}", friendRecord, expressionForDefault, 1, Optional.empty());
        assertEquals("5 9 7", message.getMessage(), "JEXL substitution evaluates correctly.");
        assertEquals("9", defaultMessage.getMessage(), "JEXL substitution evaluates correctly.");
        assertEquals(Optional.empty(), message.getChangeSpec());
    }


    @Test
    public void invalidExpression() {
        final String[] expressions = { "${child.id}, ${%%%}" };
        assertThrows(
                InvalidSyntaxException.class,
                () -> new LogMessageImpl("{0} {1}", childRecord, expressions, 1, Optional.empty()).getMessage());
    }

    @Test
    public void invalidTemplate() {
        final String[] expressions = { "${child.id}" };
        assertThrows(
                InvalidSyntaxException.class,
                () -> new LogMessageImpl("{}", childRecord, expressions, 1, Optional.empty()).getMessage());
    }

    public static class TestLoggerException extends RuntimeException {
    }

    private AuditLogger testAuditLogger = new Slf4jLogger();

    @Test
    public void threadSafetyTest() {
        final List<Throwable> exceptions = new ArrayList<>();
        final int parallelTests = 10;

        ExecutorService testThreadPool = Executors.newFixedThreadPool(parallelTests);

        for (int i = 0; i < parallelTests; i++) {
            testThreadPool.submit(() -> {
                try {
                    threadSafeLogger();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
        }

        assertTrue(exceptions.isEmpty(), exceptions.stream().map(Throwable::getMessage).findFirst().orElse(""));
    }

    public void threadSafeLogger() throws InterruptedException {
        TestLoggerException testException = new TestLoggerException();
        LogMessageImpl failMessage = new LogMessageImpl("test", 0) {
            @Override
            public String getMessage() {
                throw testException;
            }
        };

        assertSame(assertThrows(TestLoggerException.class, () -> testAuditLogger.log(failMessage)), testException);
        Thread.sleep(Math.floorMod(ThreadLocalRandom.current().nextInt(), 100));

        // should not cause another exception
        assertDoesNotThrow(() -> testAuditLogger.commit(), "Exception not cleared from previous logger commit");
    }
}
