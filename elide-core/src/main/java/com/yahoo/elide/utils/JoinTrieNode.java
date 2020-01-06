/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a structure for storing and de-duplicating elide join paths.
 * Basically, it is a Trie which uses relationship field names to navigate through the path.
 */
@Data
public class JoinTrieNode {
    private final Class<?> type;
    private final Map<String, JoinTrieNode> fields = new HashMap<>();

    public JoinTrieNode(Class<?> type) {
        this.type = type;
    }

    public void addPaths(Set<Path> paths, EntityDictionary dictionary) {
        paths.forEach(path -> addPath(path, dictionary));
    }

    /**
     * Add all path elements into this Trie, starting from the root.
     *
     * @param path full Elide join path, i.e. <code>foo.bar.baz</code>
     * @param dictionary dictionary to use.
     */
    public void addPath(Path path, EntityDictionary dictionary) {
        JoinTrieNode node = this;

        for (Path.PathElement pathElement : path.getPathElements()) {
            if (!dictionary.isRelation(pathElement.getType(), pathElement.getFieldName())) {
                break;
            }

            String fieldName = pathElement.getFieldName();

            if (!fields.containsKey(fieldName)) {
                node.addField(fieldName, new JoinTrieNode(pathElement.getFieldType()));

            }

            node = fields.get(fieldName);
        }
    }

    /**
     * Attach a field to this node.
     *
     * @param fieldName field name
     * @param node field node
     */
    private void addField(String fieldName, JoinTrieNode node) {
        fields.put(fieldName, node);
    }
}
