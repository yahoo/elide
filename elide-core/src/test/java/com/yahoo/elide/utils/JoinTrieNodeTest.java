/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;

import example.Book;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinTrieNodeTest {
    private static EntityDictionary dictionary;

    @BeforeAll
    public static void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Editor.class);
    }

    @Test
    public void testAddPath() {
        Path path = new Path(Book.class, dictionary, "publisher.editor.id");
        JoinTrieNode node = new JoinTrieNode(Book.class);
        node.addPath(path, dictionary);

        Map<String, JoinTrieNode> firstLevel = node.getFields();
        assertEquals(1, firstLevel.size());
        assertEquals(Publisher.class, firstLevel.get("publisher").getType());

        Map<String, JoinTrieNode> secondLevel = firstLevel.get("publisher").getFields();
        assertEquals(1, secondLevel.size());
        assertEquals(Editor.class, secondLevel.get("editor").getType());

        Map<String, JoinTrieNode> thirdLevel = secondLevel.get("editor").getFields();
        assertEquals(0, thirdLevel.size());
    }

    @Test
    public void testTraversal() {
        Path path = new Path(Book.class, dictionary, "publisher.editor.id");
        JoinTrieNode node = new JoinTrieNode(Book.class);
        node.addPath(path, dictionary);

        List<String> results = node.levelOrderedTraverse(
                (parentResult, childEntry) -> parentResult.getValue() + "." + childEntry.getKey(),
                (parentResult, childEntry) -> parentResult.getValue() + "." + childEntry.getKey(),
                "book"
        );

        assertEquals(2, results.size());
        assertEquals("book.publisher", results.get(0));
        assertEquals("book.publisher.editor", results.get(1));
    }
}
