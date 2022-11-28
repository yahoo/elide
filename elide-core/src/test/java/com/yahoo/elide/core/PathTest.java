/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import example.Book;
import example.Editor;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PathTest {

    @Test
    public void testIsComputed() {

        EntityDictionary dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Editor.class);

        Path computedRelationshipPath = new Path(List.of(
                new Path.PathElement(Book.class, Editor.class, "editor"),
                new Path.PathElement(Editor.class, String.class, "firstName")
        ));

        Path computedAttributePath = new Path(List.of(
                new Path.PathElement(Editor.class, String.class, "fullName")
        ));

        Path attributePath = new Path(List.of(
                new Path.PathElement(Book.class, String.class, "title")
        ));

        assertTrue(computedRelationshipPath.isComputed(dictionary));
        assertTrue(computedAttributePath.isComputed(dictionary));
        assertFalse(attributePath.isComputed(dictionary));
    }
}
