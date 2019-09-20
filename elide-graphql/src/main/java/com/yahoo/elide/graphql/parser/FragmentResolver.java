/*
 * Copyright 2019, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;

import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class that fetch {@link FragmentDefinition}s from graphQL {@link Document} and store them for future reference.
 */
class FragmentResolver {
    private final Map<String, FragmentDefinition> fragmentMap = new HashMap<>();

    boolean contains(String fragmentName) {
        return fragmentMap.containsKey(fragmentName);
    }

    FragmentDefinition get(String fragmentName) {
        return fragmentMap.get(fragmentName);
    }

    /**
     * Fetch fragments from documents. Only fragment definitions would be processed.
     *
     * @param document graphql document
     */
    void addFragments(Document document) {
        addFragments(document.getDefinitions().stream()
                .filter(definition -> definition instanceof FragmentDefinition)
                .map(definition -> (FragmentDefinition) definition)
                .collect(Collectors.toList()));
    }

    /**
     * Make sure there is not fragment loop in in-coming definitions and store those fragments.
     *
     * @param fragments fragments to add
     */
    private void addFragments(List<FragmentDefinition> fragments) {
        final Map<String, FragmentDefinition> newFragments = fragments.stream()
                .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));

        // make sure there is no fragment loop and undefined fragments in fragment definitions
        final Set<String> fragmentNames = new HashSet<>();
        fragments.forEach(fragmentDefinition -> validateFragment(newFragments, fragmentDefinition, fragmentNames));

        this.fragmentMap.putAll(newFragments);
    }

    /**
     * Recursive DFS to validate that there is not reference loop in a fragment and there is not un-defined
     * fragments.
     *
     * @param fragmentDefinition fragment to be checked
     * @param fragmentNames fragment names appear in the current check path
     */
    private static void validateFragment(
            Map<String, FragmentDefinition> fragmentMap,
            FragmentDefinition fragmentDefinition,
            Set<String> fragmentNames
    ) {
        String fragmentName = fragmentDefinition.getName();
        if (fragmentNames.contains(fragmentName)) {
            throw new InvalidEntityBodyException("There is a fragment definition loop in: {"
                    + String.join(",", fragmentNames) + "} with " + fragmentName + " duplicated.");
        }

        fragmentNames.add(fragmentName);

        fragmentDefinition.getSelectionSet().getSelections().stream()
                .filter(selection -> selection instanceof FragmentSpread)
                .map(fragment -> ((FragmentSpread) fragment).getName())
                .distinct()
                .forEach(name -> {
                    if (!fragmentMap.containsKey(name)) {
                        throw new InvalidEntityBodyException(String.format("Unknown fragment {%s}.", name));
                    }
                    validateFragment(fragmentMap, fragmentMap.get(name), fragmentNames);
                });

        fragmentNames.remove(fragmentName);
    }
}
