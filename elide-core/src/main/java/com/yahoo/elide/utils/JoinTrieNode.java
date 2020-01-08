/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;

import javafx.util.Pair;
import lombok.Data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

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
            Class<?> entityClass = pathElement.getType();
            String fieldName = pathElement.getFieldName();

            if (!dictionary.isRelation(entityClass, fieldName)) {
                break;
            }

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

    /**
     * Traverse this Trie and project the result into a list in level-first-order.
     * This previous result-node pair would be carried through the traversal.
     *
     * @param generator function that generate new results from previous result-node pair and new trie field
     * @param traverser function that carry previous result for next level traversal
     * @param identity initial result value
     * @param <T> type of each individual result
     * @return resulted projected in a list in level-first-order.
     */
    public <T> List<T> levelOrderedTraverse(
            BiFunction<Pair<JoinTrieNode, T>, Map.Entry<String, JoinTrieNode>, T> generator,
            BiFunction<Pair<JoinTrieNode, T>, Map.Entry<String, JoinTrieNode>, T> traverser,
            T identity
    ) {
        // node-result pairs queue
        Queue<Pair<JoinTrieNode, T>> todo = new ArrayDeque<>();

        todo.add(new Pair<>(this, identity));
        List<T> results = new ArrayList<>();

        while (!todo.isEmpty()) {
            Pair<JoinTrieNode, T> parentResult = todo.remove();

            parentResult.getKey().getFields().entrySet().forEach(childField -> {
                results.add(generator.apply(parentResult, childField));

                if (childField.getValue().getFields().size() > 0) {
                    todo.add(new Pair<>(childField.getValue(), traverser.apply(parentResult, childField)));
                }
            });
        }

        return results;
    }
}
