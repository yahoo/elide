/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.InPredicate;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;

public class FilterPredicatePushdownExtractorTest {

    private EntityDictionary dictionary;

    @BeforeTest
    public void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(Publisher.class);
    }

    @Test
    public void testFullPredicateExtraction() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, expression);

        Assert.assertEquals(extracted, expression);
    }

    @Test
    public void testNoPredicateExtraction() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Literary Fiction");

        FilterExpression finalExpression = new NotFilterExpression(new AndFilterExpression(dataStoreExpression, inMemoryExpression));

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, finalExpression);

        Assert.assertNull(extracted);
    }

    @Test
    public void testPartialPredicateExtraction() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Literary Fiction");

        FilterExpression finalExpression = new NotFilterExpression(new OrFilterExpression(dataStoreExpression, inMemoryExpression));

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, finalExpression);

        Assert.assertEquals(extracted, ((InPredicate) dataStoreExpression).negate());
    }
}
