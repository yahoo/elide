/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.type.ClassType;
import example.Book;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;

public class EqualityTest {

    @Test
    public void testPredicateEquality() throws Exception {
        TestDictionary testDictionary = new TestDictionary(EntityDictionary.DEFAULT_INJECTOR, new HashMap<>());
        testDictionary.bindEntity(Book.class);

        RSQLFilterDialect dialect = RSQLFilterDialect.builder().dictionary(testDictionary).build();

        FilterExpression a = dialect.parse(ClassType.of(Book.class), new HashSet<>(), "title=='Foo'", NO_VERSION);
        FilterExpression b = dialect.parse(ClassType.of(Book.class), new HashSet<>(), "title=='Foo'", NO_VERSION);

        assertTrue(a.equals(b));
    }

    @Test
    public void testPredicateInequality() throws Exception {
        TestDictionary testDictionary = new TestDictionary(EntityDictionary.DEFAULT_INJECTOR, new HashMap<>());
        testDictionary.bindEntity(Book.class);

        RSQLFilterDialect dialect = RSQLFilterDialect.builder().dictionary(testDictionary).build();

        FilterExpression a = dialect.parse(ClassType.of(Book.class), new HashSet<>(), "title!='Foo'", NO_VERSION);
        FilterExpression b = dialect.parse(ClassType.of(Book.class), new HashSet<>(), "title=='Foo'", NO_VERSION);

        assertFalse(a.equals(b));
    }

    @Test
    public void testAndEquality() throws Exception {
        TestDictionary testDictionary = new TestDictionary(EntityDictionary.DEFAULT_INJECTOR, new HashMap<>());
        testDictionary.bindEntity(Book.class);

        RSQLFilterDialect dialect = RSQLFilterDialect.builder().dictionary(testDictionary).build();

        FilterExpression a = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo';title=='Foo'", NO_VERSION);
        FilterExpression b = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo';title=='Foo'", NO_VERSION);
        FilterExpression c = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title=='Foo';title!='Foo'", NO_VERSION);

        assertTrue(a.equals(b));
        assertTrue(a.equals(c));
        assertTrue(b.equals(c));
    }

    @Test
    public void testOrEquality() throws Exception {
        TestDictionary testDictionary = new TestDictionary(EntityDictionary.DEFAULT_INJECTOR, new HashMap<>());
        testDictionary.bindEntity(Book.class);

        RSQLFilterDialect dialect = RSQLFilterDialect.builder().dictionary(testDictionary).build();

        FilterExpression a = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo',title=='Foo'", NO_VERSION);
        FilterExpression b = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo',title=='Foo'", NO_VERSION);
        FilterExpression c = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title=='Foo',title!='Foo'", NO_VERSION);

        assertTrue(a.equals(b));
        assertTrue(a.equals(c));
        assertTrue(b.equals(c));
    }

    @Test
    public void testOrInequality() throws Exception {
        TestDictionary testDictionary = new TestDictionary(EntityDictionary.DEFAULT_INJECTOR, new HashMap<>());
        testDictionary.bindEntity(Book.class);

        RSQLFilterDialect dialect = RSQLFilterDialect.builder().dictionary(testDictionary).build();

        FilterExpression a = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo',title=='Foo'", NO_VERSION);
        FilterExpression b = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo',title=='Bar'", NO_VERSION);
        FilterExpression c = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title=='Foo',title!='Foo'", NO_VERSION);

        assertFalse(a.equals(b));
        assertFalse(b.equals(c));
    }

    @Test
    public void testAndInequality() throws Exception {
        TestDictionary testDictionary = new TestDictionary(EntityDictionary.DEFAULT_INJECTOR, new HashMap<>());
        testDictionary.bindEntity(Book.class);

        RSQLFilterDialect dialect = RSQLFilterDialect.builder().dictionary(testDictionary).build();

        FilterExpression a = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo';title=='Foo'", NO_VERSION);
        FilterExpression b = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title!='Foo';title=='Bar'", NO_VERSION);
        FilterExpression c = dialect.parse(ClassType.of(Book.class), new HashSet<>(),
                "title=='Foo';title!='Foo'", NO_VERSION);

        assertFalse(a.equals(b));
        assertFalse(b.equals(c));
    }
}
